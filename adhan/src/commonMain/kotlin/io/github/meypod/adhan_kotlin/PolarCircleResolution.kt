package io.github.meypod.adhan_kotlin

import io.github.meypod.adhan_kotlin.data.CalendarUtil
import io.github.meypod.adhan_kotlin.data.DateComponents
import io.github.meypod.adhan_kotlin.internal.SolarTime
import kotlinx.datetime.DateTimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign

enum class PolarCircleResolution {
  AqrabBalad,
  AqrabYaum,
  Unresolved
}

private const val LATITUDE_VARIATION_STEP = 0.5 // Degrees to add/remove at each resolution step
private const val UNSAFE_LATITUDE = 65.0 // Based on https://en.wikipedia.org/wiki/Midnight_sun

data class PolarResolvedValues(
  val date: DateComponents,
  val tomorrow: DateComponents,
  val coordinates: Coordinates,
  val solarTime: SolarTime,
  val tomorrowSolarTime: SolarTime,
)

private fun isValidSolarTime(solarTime: SolarTime): Boolean =
  !solarTime.sunrise.isNaN() && !solarTime.sunset.isNaN()

private fun dateByAddingDays(date: DateComponents, days: Int): DateComponents {
  val local = CalendarUtil.resolveTime(date)
  val updated = CalendarUtil.add(local, days, DateTimeUnit.DAY)
  return DateComponents.fromLocalDateTime(updated)
}

private fun aqrabYaumResolver(
  coordinates: Coordinates,
  date: DateComponents,
  interpolateDeclination: Boolean,
  daysAdded: Int = 1,
  direction: Int = 1,
): PolarResolvedValues? {
  if (daysAdded > ceil(365.0 / 2.0).toInt()) return null

  val testDate = dateByAddingDays(date, direction * daysAdded)
  val tomorrow = dateByAddingDays(testDate, 1)
  val solarTime = SolarTime(testDate, coordinates, interpolateDeclination)
  val tomorrowSolarTime = SolarTime(tomorrow, coordinates, interpolateDeclination)

  if (!isValidSolarTime(solarTime) || !isValidSolarTime(tomorrowSolarTime)) {
    val nextDaysAdded = daysAdded + if (direction > 0) 0 else 1
    return aqrabYaumResolver(coordinates, date, interpolateDeclination, nextDaysAdded, -direction)
  }

  return PolarResolvedValues(date, tomorrow, coordinates, solarTime, tomorrowSolarTime)
}

private fun aqrabBaladResolver(
  coordinates: Coordinates,
  date: DateComponents,
  latitude: Double,
  interpolateDeclination: Boolean,
): PolarResolvedValues? {
  val testCoords = Coordinates(latitude, coordinates.longitude)
  val solarTime = SolarTime(date, testCoords, interpolateDeclination)
  val tomorrow = dateByAddingDays(date, 1)
  val tomorrowSolarTime = SolarTime(tomorrow, testCoords, interpolateDeclination)

  if (!isValidSolarTime(solarTime) || !isValidSolarTime(tomorrowSolarTime)) {
    return if (abs(latitude) >= UNSAFE_LATITUDE) {
      aqrabBaladResolver(
        coordinates,
        date,
        latitude - sign(latitude) * LATITUDE_VARIATION_STEP,
        interpolateDeclination,
      )
    } else {
      null
    }
  }

  return PolarResolvedValues(
    date,
    tomorrow,
    Coordinates(latitude, coordinates.longitude),
    solarTime,
    tomorrowSolarTime,
  )
}

fun resolvePolarCircleValues(
    resolver: PolarCircleResolution,
    date: DateComponents,
    coordinates: Coordinates,
): PolarResolvedValues = resolvePolarCircleValues(resolver, date, coordinates, true)

fun resolvePolarCircleValues(
    resolver: PolarCircleResolution,
    date: DateComponents,
    coordinates: Coordinates,
    interpolateDeclination: Boolean,
): PolarResolvedValues {
  val tomorrow = dateByAddingDays(date, 1)
  val defaultReturn = PolarResolvedValues(
    date,
    tomorrow,
    coordinates,
    SolarTime(date, coordinates, interpolateDeclination),
    SolarTime(tomorrow, coordinates, interpolateDeclination),
  )

  return when (resolver) {
    PolarCircleResolution.AqrabYaum ->
      aqrabYaumResolver(coordinates, date, interpolateDeclination) ?: defaultReturn
    PolarCircleResolution.AqrabBalad -> {
      val latitude = coordinates.latitude
      aqrabBaladResolver(
        coordinates,
        date,
        latitude - sign(latitude) * LATITUDE_VARIATION_STEP,
        interpolateDeclination,
      ) ?: defaultReturn
    }
    else -> defaultReturn
  }
}
