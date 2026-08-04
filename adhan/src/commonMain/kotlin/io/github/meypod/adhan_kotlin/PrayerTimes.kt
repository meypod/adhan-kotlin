package io.github.meypod.adhan_kotlin

import io.github.meypod.adhan_kotlin.Prayer.ASR
import io.github.meypod.adhan_kotlin.Prayer.DHUHR
import io.github.meypod.adhan_kotlin.Prayer.FAJR
import io.github.meypod.adhan_kotlin.Prayer.ISHA
import io.github.meypod.adhan_kotlin.Prayer.MAGHRIB
import io.github.meypod.adhan_kotlin.Prayer.NONE
import io.github.meypod.adhan_kotlin.Prayer.SUNRISE
import io.github.meypod.adhan_kotlin.data.CalendarUtil
import io.github.meypod.adhan_kotlin.data.CalendarUtil.add
import io.github.meypod.adhan_kotlin.data.CalendarUtil.isLeapYear
import io.github.meypod.adhan_kotlin.data.CalendarUtil.resolveTime
import io.github.meypod.adhan_kotlin.data.CalendarUtil.roundedMinute
import io.github.meypod.adhan_kotlin.data.CalendarUtil.toUtcInstant
import io.github.meypod.adhan_kotlin.data.DateComponents
import io.github.meypod.adhan_kotlin.data.TimeComponents
import io.github.meypod.adhan_kotlin.internal.SolarTime
import io.github.meypod.adhan_kotlin.internal.TakdirTable
import io.github.meypod.adhan_kotlin.model.Shafaq
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/**
 * Calculate PrayerTimes
 * @param coordinates the coordinates of the location
 * @param dateComponents the date components for that location
 * @param calculationParameters the parameters for the calculation
 */
data class PrayerTimes(
  val coordinates: Coordinates,
  val dateComponents: DateComponents,
  val calculationParameters: CalculationParameters
) {
  val fajr: Instant
  val sunrise: Instant
  val dhuhr: Instant
  val asr: Instant
  val sunset: Instant
  val maghrib: Instant
  val isha: Instant

  /**
   * This coordinate can differ from the original passed coordinate only
   * if the polar resolution method is [PolarCircleResolution.AqrabBalad]
   */
  val effectiveCoordinates: Coordinates

  init {
    var coordinates = coordinates
    var tempFajr: LocalDateTime? = null
    val tempSunrise: LocalDateTime?
    val tempDhuhr: LocalDateTime?
    var tempAsr: LocalDateTime? = null
    val tempSunset: LocalDateTime?
    var tempMaghrib: LocalDateTime?
    var tempIsha: LocalDateTime? = null
    val prayerDate: LocalDateTime = resolveTime(dateComponents)

    val dayOfYear: Int = prayerDate.dayOfYear

    val tomorrowDate: LocalDateTime = add(prayerDate, 1, DateTimeUnit.DAY)
    var tomorrow: DateComponents = DateComponents.fromLocalDateTime(tomorrowDate)

    val interpolateDeclination = calculationParameters.interpolateDeclination
    var solarTime = SolarTime(dateComponents, coordinates, interpolateDeclination)
    var timeComponents = TimeComponents.fromDouble(solarTime.transit)
    var transit = timeComponents?.dateComponents(dateComponents)

    timeComponents = TimeComponents.fromDouble(solarTime.sunrise)
    var sunriseComponents = timeComponents?.dateComponents(dateComponents)

    timeComponents = TimeComponents.fromDouble(solarTime.sunset)
    var sunsetComponents = timeComponents?.dateComponents(dateComponents)

    var tomorrowSolarTime = SolarTime(tomorrow, coordinates, interpolateDeclination)
    var tomorrowSunriseComponents = TimeComponents.fromDouble(tomorrowSolarTime.sunrise)

    val polarCircleResolver = calculationParameters.polarCircleResolution
    if ((sunriseComponents == null || sunsetComponents == null || tomorrowSolarTime.sunrise.isNaN())
      && polarCircleResolver != PolarCircleResolution.Unresolved) {
      val resolved = resolvePolarCircleValues(
        polarCircleResolver, dateComponents, coordinates, interpolateDeclination
      )

      coordinates = resolved.coordinates
      solarTime = resolved.solarTime
      tomorrow = resolved.tomorrow

      timeComponents = TimeComponents.fromDouble(solarTime.transit)
      transit = timeComponents?.dateComponents(dateComponents)

      timeComponents = TimeComponents.fromDouble(solarTime.sunrise)
      sunriseComponents = timeComponents?.dateComponents(dateComponents)

      timeComponents = TimeComponents.fromDouble(solarTime.sunset)
      sunsetComponents = timeComponents?.dateComponents(dateComponents)

      tomorrowSolarTime = resolved.tomorrowSolarTime
      tomorrowSunriseComponents = TimeComponents.fromDouble(tomorrowSolarTime.sunrise)
    }

    var fiveHourFloorApplied = false
    // Diyanet floors both the day and the night at five hours, placing sunrise and maghrib
    // symmetrically about Ogle. This defines them outright inside the polar circle, where the
    // sun may not rise or set at all, so it is applied before anything derived from them.
    if (calculationParameters.effectiveHighLatitudeRule(coordinates) ===
        HighLatitudeRule.PROPORTIONAL_DEPRESSION && transit != null) {
      val sunriseShift = calculationParameters.methodAdjustments.sunrise +
          calculationParameters.prayerAdjustments.sunrise
      val maghribShift = calculationParameters.methodAdjustments.maghrib +
          calculationParameters.prayerAdjustments.maghrib
      val dhuhrShift = calculationParameters.methodAdjustments.dhuhr +
          calculationParameters.prayerAdjustments.dhuhr

      // the published day, i.e. after the temkin has been applied to both ends
      val publishedDay = if (sunriseComponents == null || sunsetComponents == null) {
        // the sun never crosses the horizon: a polar night is a zero-length day, a polar day a
        // zero-length night
        if (solarTime.maximumAltitude < 0.0) 0 else 1440
      } else {
        minutesBetween(sunriseComponents, sunsetComponents) + maghribShift - sunriseShift
      }

      val halfFromDhuhr = when {
        publishedDay < TakdirTable.MINIMUM_DAY_MINUTES -> TakdirTable.FLOORED_DAY_HALF_MINUTES
        1440 - publishedDay < TakdirTable.MINIMUM_DAY_MINUTES ->
          TakdirTable.FLOORED_NIGHT_HALF_MINUTES
        else -> null
      }

      if (halfFromDhuhr != null) {
        fiveHourFloorApplied = true
        // solve so that the *published* sunrise and maghrib land halfFromDhuhr either side of Ogle
        sunriseComponents = add(transit, dhuhrShift - halfFromDhuhr - sunriseShift, DateTimeUnit.MINUTE)
        sunsetComponents = add(transit, dhuhrShift + halfFromDhuhr - maghribShift, DateTimeUnit.MINUTE)
        tomorrowSunriseComponents = TimeComponents.fromLocalDateTime(
          add(sunriseComponents, 1440, DateTimeUnit.MINUTE)
        )
      }
    }

    effectiveCoordinates = coordinates

    if (transit == null || sunriseComponents == null || sunsetComponents == null || tomorrowSunriseComponents == null) {
      tempSunrise = null
      tempDhuhr = null
      tempAsr = null
      tempSunset = null
      tempMaghrib = null
    } else {
      tempDhuhr = transit
      tempSunrise = sunriseComponents
      tempSunset = sunsetComponents
      tempMaghrib = sunsetComponents
      timeComponents = TimeComponents.fromDouble(
        solarTime.afternoon(calculationParameters.madhab.shadowLength)
      )

      if (timeComponents != null) {
        tempAsr = timeComponents.dateComponents(dateComponents)
      }

      // The asr shadow ratio can be unattainable at extreme latitudes: where the sun never rises
      // the shadow formula returns a meaningless value, and in a polar day at very high latitudes
      // the sun circles too high ever to cast it. Diyanet publishes asr equal to dhuhr on exactly
      // the days its sun never rises (58 of them at Tromso; none at Oulu, whose sun always clears
      // the horizon), and the same fallback keeps the times ordered in the other case.
      if (calculationParameters.effectiveHighLatitudeRule(coordinates) ===
          HighLatitudeRule.PROPORTIONAL_DEPRESSION) {
        val asrShift = calculationParameters.methodAdjustments.asr +
            calculationParameters.prayerAdjustments.asr
        val dhuhrShiftForAsr = calculationParameters.methodAdjustments.dhuhr +
            calculationParameters.prayerAdjustments.dhuhr
        val maghribShiftForAsr = calculationParameters.methodAdjustments.maghrib +
            calculationParameters.prayerAdjustments.maghrib
        // offsets chosen so the comparison holds once each prayer has its own adjustment applied
        val asrFloor = add(transit, dhuhrShiftForAsr - asrShift, DateTimeUnit.MINUTE)
        val asrCeiling = add(sunsetComponents, maghribShiftForAsr - asrShift, DateTimeUnit.MINUTE)
        tempAsr = when {
          solarTime.maximumAltitude < 0.0 || tempAsr == null -> asrFloor
          tempAsr.before(asrFloor) -> asrFloor
          tempAsr.after(asrCeiling) -> asrCeiling
          else -> tempAsr
        }
      }

      // get night length
      // we recreate tomorrow from today, because polar resolution may change that
      val tomorrowSunrise = tomorrowSunriseComponents.dateComponents(DateComponents.fromLocalDateTime(add(prayerDate, 1, DateTimeUnit.DAY)))
      val night = tomorrowSunrise.toInstant(TimeZone.UTC).toEpochMilliseconds() -
          sunsetComponents.toInstant(TimeZone.UTC).toEpochMilliseconds()

      timeComponents = TimeComponents.fromDouble(
        solarTime.timeForSolarAngle(-calculationParameters.fajrAngle, false))
      if (timeComponents != null) {
        tempFajr = timeComponents.dateComponents(dateComponents)
      }

      // special case for moonsighting committee above latitude 55
      if (calculationParameters.method === CalculationMethod.MOON_SIGHTING_COMMITTEE &&
        coordinates.latitude >= 55
      ) {
        tempFajr = add(
          sunriseComponents, -1 * (night / 7000).toInt(), DateTimeUnit.SECOND
        )
      }

      val nightPortions = calculationParameters.nightPortions(coordinates)
      val usesTakdir = calculationParameters.effectiveHighLatitudeRule(coordinates) ===
          HighLatitudeRule.PROPORTIONAL_DEPRESSION
      // Diyanet only estimates from 44.5 degrees latitude upwards, its own published threshold;
      // south of it the plain twilight angles are published as-is, with no bound at all.
      val takdirApplies = usesTakdir && abs(coordinates.latitude) >= TakdirTable.MIN_LATITUDE

      val nightPortionFajr = add(
        sunriseComponents,
        -1 * (nightPortions.fajr * night / 1000).toLong().toInt(),
        DateTimeUnit.SECOND
      )

      // null means "leave the angle-based time alone"
      val safeFajr: LocalDateTime? = when {
        calculationParameters.method === CalculationMethod.MOON_SIGHTING_COMMITTEE ->
          seasonAdjustedMorningTwilight(
            coordinates.latitude,
            dayOfYear,
            dateComponents.year,
            sunriseComponents
          )
        // between the 45th and 46th parallels Diyanet's estimate holds the twilight duration
        // itself close to constant, rather than a proportion of the sun's descent
        // where the five-hour floor has replaced the geometry, the twilight follows the floored
        // night rather than any solar angle
        takdirApplies && fiveHourFloorApplied -> nightPortionFajr
        takdirApplies && abs(coordinates.latitude) < TakdirTable.MAX_DURATION_LATITUDE -> add(
          sunriseComponents,
          -TakdirTable.fajrDurationCap(coordinates.latitude),
          DateTimeUnit.MINUTE
        )
        takdirApplies -> TimeComponents.fromDouble(
          solarTime.timeForProportionalDepression(
            calculationParameters.fajrAngle,
            TakdirTable.fajrProportion(coordinates.latitude),
            false,
            TakdirTable.usesMeanSolarTime(coordinates.latitude)
          )
        )?.dateComponents(dateComponents) ?: nightPortionFajr
        usesTakdir -> null
        else -> nightPortionFajr
      }

      if (tempFajr == null) {
        tempFajr = safeFajr ?: nightPortionFajr
      } else if (safeFajr != null && tempFajr.before(safeFajr)) {
        tempFajr = safeFajr
      }

      // Isha calculation with check against safe value
      if (calculationParameters.ishaInterval > 0) {
        tempIsha = add(tempMaghrib, calculationParameters.ishaInterval * 60, DateTimeUnit.SECOND)
      } else {
        timeComponents = TimeComponents.fromDouble(
          solarTime.timeForSolarAngle(-calculationParameters.ishaAngle, true)
        )
        if (timeComponents != null) {
          tempIsha = timeComponents.dateComponents(dateComponents)
        }

        // special case for moonsighting committee above latitude 55
        if (calculationParameters.method === CalculationMethod.MOON_SIGHTING_COMMITTEE &&
          coordinates.latitude >= 55
        ) {
          val nightFraction = night / 7000
          tempIsha = add(sunsetComponents, nightFraction.toInt(), DateTimeUnit.SECOND)
        }

        val nightPortionIsha = add(
          sunsetComponents,
          (nightPortions.isha * night / 1000).toLong().toInt(),
          DateTimeUnit.SECOND
        )

        val safeIsha: LocalDateTime? = when {
          calculationParameters.method === CalculationMethod.MOON_SIGHTING_COMMITTEE ->
            seasonAdjustedEveningTwilight(
              coordinates.latitude, dayOfYear, dateComponents.year, sunsetComponents,
              calculationParameters.shafaq
            )
          // where the five-hour floor has replaced the geometry, the twilight follows the floored
          // night rather than any solar angle
          takdirApplies && fiveHourFloorApplied -> nightPortionIsha
          takdirApplies && abs(coordinates.latitude) < TakdirTable.MAX_DURATION_LATITUDE -> add(
            sunsetComponents,
            TakdirTable.ishaDurationCap(coordinates.latitude),
            DateTimeUnit.MINUTE
          )
          takdirApplies -> TimeComponents.fromDouble(
            solarTime.timeForProportionalDepression(
              calculationParameters.ishaAngle,
              TakdirTable.ishaProportion(coordinates.latitude),
              true,
              TakdirTable.usesMeanSolarTime(coordinates.latitude)
            )
          )?.dateComponents(dateComponents) ?: nightPortionIsha
          usesTakdir -> null
          else -> nightPortionIsha
        }

        if (tempIsha == null) {
          tempIsha = safeIsha ?: nightPortionIsha
        } else if (safeIsha != null && tempIsha.after(safeIsha)) {
          tempIsha = safeIsha
        }
      }

      if (calculationParameters.maghribAngle > 0.0) {
        timeComponents = TimeComponents.fromDouble(
          solarTime.timeForSolarAngle(-calculationParameters.maghribAngle, true)
        )
        if (timeComponents != null) {
          val angleBasedMaghrib = timeComponents.dateComponents(dateComponents)
          if (
            tempSunset.before(angleBasedMaghrib) &&
            tempIsha.after(angleBasedMaghrib)
          ) {
            tempMaghrib = angleBasedMaghrib
          }
        }
      }
    }

    if (tempFajr == null || tempSunrise == null || tempSunset == null || tempDhuhr == null || tempAsr == null || tempMaghrib == null || tempIsha == null) {
      // if we don't have all prayer times then initialization failed
      throw IllegalStateException()
    } else {
      // Assign final times to public struct members with all offsets
      fajr = roundedMinute(
        add(
          add(tempFajr, calculationParameters.prayerAdjustments.fajr, DateTimeUnit.MINUTE),
          calculationParameters.methodAdjustments.fajr,
          DateTimeUnit.MINUTE
        ),
        rounding = calculationParameters.rounding
      ).toUtcInstant()
      sunrise = roundedMinute(
        add(
          add(tempSunrise, calculationParameters.prayerAdjustments.sunrise, DateTimeUnit.MINUTE),
          calculationParameters.methodAdjustments.sunrise,
          DateTimeUnit.MINUTE
        ),
        rounding = calculationParameters.rounding
      ).toUtcInstant()
      dhuhr = roundedMinute(
        add(
          add(tempDhuhr, calculationParameters.prayerAdjustments.dhuhr, DateTimeUnit.MINUTE),
          calculationParameters.methodAdjustments.dhuhr,
          DateTimeUnit.MINUTE
        ),
        rounding = calculationParameters.rounding
      ).toUtcInstant()
      asr = roundedMinute(
        add(
          add(tempAsr, calculationParameters.prayerAdjustments.asr, DateTimeUnit.MINUTE),
          calculationParameters.methodAdjustments.asr,
          DateTimeUnit.MINUTE
        ),
        rounding = calculationParameters.rounding
      ).toUtcInstant()
      maghrib = roundedMinute(
        add(
          add(tempMaghrib, calculationParameters.prayerAdjustments.maghrib, DateTimeUnit.MINUTE),
          calculationParameters.methodAdjustments.maghrib,
          DateTimeUnit.MINUTE
        ),
        rounding = calculationParameters.rounding
      ).toUtcInstant()
      sunset = roundedMinute(
          add(
              add(tempSunset, calculationParameters.prayerAdjustments.sunset, DateTimeUnit.MINUTE),
              calculationParameters.methodAdjustments.sunset,
              DateTimeUnit.MINUTE
          ),
          rounding = calculationParameters.rounding
      ).toUtcInstant()
      isha = roundedMinute(
        add(
          add(tempIsha, calculationParameters.prayerAdjustments.isha, DateTimeUnit.MINUTE),
          calculationParameters.methodAdjustments.isha,
          DateTimeUnit.MINUTE
        ),
        rounding = calculationParameters.rounding
      ).toUtcInstant()
    }
  }

  fun currentPrayer(instant: Instant): Prayer {
    return when {
      instant >= isha -> { ISHA }
      instant >= maghrib -> { MAGHRIB }
      instant >= asr -> { ASR }
      instant >= dhuhr -> { DHUHR }
      instant >= sunrise -> { SUNRISE }
      instant >= fajr -> { FAJR }
      else -> { NONE }
    }
  }

  fun nextPrayer(instant: Instant): Prayer {
    return when {
      instant >= isha -> { NONE }
      instant >= maghrib -> { ISHA }
      instant >= asr -> { MAGHRIB }
      instant >= dhuhr -> { ASR }
      instant >= sunrise -> { DHUHR }
      instant >= fajr -> { SUNRISE }
      else -> { FAJR }
    }
  }

  fun timeForPrayer(prayer: Prayer): Instant? {
    return when (prayer) {
      FAJR -> fajr
      SUNRISE -> sunrise
      DHUHR -> dhuhr
      ASR -> asr
      MAGHRIB -> maghrib
      ISHA -> isha
      NONE -> null
    }
  }

  private fun minutesBetween(from: LocalDateTime, to: LocalDateTime): Int =
    ((to.toInstant(TimeZone.UTC).toEpochMilliseconds() -
      from.toInstant(TimeZone.UTC).toEpochMilliseconds()) / 60000L).toInt()

  private fun LocalDateTime.before(other: LocalDateTime): Boolean {
    return toInstant(TimeZone.UTC).toEpochMilliseconds() <
        other.toInstant(TimeZone.UTC).toEpochMilliseconds()
  }

  private fun LocalDateTime.after(other: LocalDateTime): Boolean {
    return toInstant(TimeZone.UTC).toEpochMilliseconds() >
        other.toInstant(TimeZone.UTC).toEpochMilliseconds()
  }

  companion object {
    private fun seasonAdjustedMorningTwilight(
      latitude: Double, day: Int, year: Int, sunrise: LocalDateTime
    ): LocalDateTime {
      val a: Double = 75 + 28.65 / 55.0 * abs(latitude)
      val b: Double = 75 + 19.44 / 55.0 * abs(latitude)
      val c: Double = 75 + 32.74 / 55.0 * abs(latitude)
      val d: Double = 75 + 48.10 / 55.0 * abs(latitude)
      val dyy = daysSinceSolstice(day, year, latitude)
      val adjustment = when {
        dyy < 91 -> { a + (b - a) / 91.0 * dyy }
        dyy < 137 -> { b + (c - b) / 46.0 * (dyy - 91) }
        dyy < 183 -> { c + (d - c) / 46.0 * (dyy - 137) }
        dyy < 229 -> { d + (c - d) / 46.0 * (dyy - 183) }
        dyy < 275 -> { c + (b - c) / 46.0 * (dyy - 229) }
        else -> { b + (a - b) / 91.0 * (dyy - 275) }
      }
      return add(sunrise, -(adjustment * 60.0).roundToInt(), DateTimeUnit.SECOND)
    }

    private fun seasonAdjustedEveningTwilight(
      latitude: Double, day: Int, year: Int, sunset: LocalDateTime, shafaq: Shafaq
    ): LocalDateTime {

      val a: Double
      val b: Double
      val c: Double
      val d: Double
      when (shafaq) {
        Shafaq.GENERAL -> {
          a = 75 + 25.60 / 55.0 * abs(latitude)
          b = 75 + 2.050 / 55.0 * abs(latitude)
          c = 75 - 9.210 / 55.0 * abs(latitude)
          d = 75 + 6.140 / 55.0 * abs(latitude)
        }
        Shafaq.AHMER -> {
          a = 62 + ((17.40 / 55.0) * abs(latitude))
          b = 62 - ((7.160 / 55.0) * abs(latitude))
          c = 62 + ((5.120 / 55.0) * abs(latitude))
          d = 62 + ((19.44 / 55.0) * abs(latitude))
        }
        Shafaq.ABYAD -> {
          a = 75 + ((25.60 / 55.0) * abs(latitude))
          b = 75 + ((7.160 / 55.0) * abs(latitude))
          c = 75 + ((36.84 / 55.0) * abs(latitude))
          d = 75 + ((81.84 / 55.0) * abs(latitude))
        }
      }

      val dyy = daysSinceSolstice(day, year, latitude)
      val adjustment = when {
        dyy < 91 -> { a + (b - a) / 91.0 * dyy }
        dyy < 137 -> { b + (c - b) / 46.0 * (dyy - 91) }
        dyy < 183 -> { c + (d - c) / 46.0 * (dyy - 137) }
        dyy < 229 -> { d + (c - d) / 46.0 * (dyy - 183) }
        dyy < 275 -> { c + (b - c) / 46.0 * (dyy - 229) }
        else -> { b + (a - b) / 91.0 * (dyy - 275) }
      }
      return add(sunset, (adjustment * 60.0).roundToInt(), DateTimeUnit.SECOND)
    }

    fun daysSinceSolstice(dayOfYear: Int, year: Int, latitude: Double): Int {
      var daysSinceSolistice: Int
      val northernOffset = 10
      val isLeapYear = isLeapYear(year)
      val southernOffset = if (isLeapYear) 173 else 172
      val daysInYear = if (isLeapYear) 366 else 365
      if (latitude >= 0) {
        daysSinceSolistice = dayOfYear + northernOffset
        if (daysSinceSolistice >= daysInYear) {
          daysSinceSolistice -= daysInYear
        }
      } else {
        daysSinceSolistice = dayOfYear - southernOffset
        if (daysSinceSolistice < 0) {
          daysSinceSolistice += daysInYear
        }
      }
      return daysSinceSolistice
    }
  }
}
