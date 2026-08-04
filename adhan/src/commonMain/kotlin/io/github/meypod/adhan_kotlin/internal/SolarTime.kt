package io.github.meypod.adhan_kotlin.internal

import io.github.meypod.adhan_kotlin.Coordinates
import io.github.meypod.adhan_kotlin.data.DateComponents
import io.github.meypod.adhan_kotlin.internal.Astronomical.approximateTransit
import io.github.meypod.adhan_kotlin.internal.Astronomical.correctedHourAngle
import io.github.meypod.adhan_kotlin.internal.Astronomical.correctedTransit
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.tan

class SolarTime(
  date: DateComponents,
  coordinates: Coordinates,
  private val interpolateDeclination: Boolean = true
) {

  companion object {
    /** Depression of the sun's centre at sunrise/sunset, accounting for refraction and radius. */
    private const val SUNRISE_DEPRESSION = 50.0 / 60.0
  }

  val transit: Double
  val sunrise: Double
  val sunset: Double

  /**
   * How far the sun descends below the horizon at solar midnight, in degrees.
   * Negative when the sun stays above the horizon all night.
   */
  val maximumDepression: Double
    get() = 90.0 - abs(observer.latitude + solar.declination)

  /** The sun's altitude at transit, in degrees. Negative where the sun never rises. */
  val maximumAltitude: Double
    get() = 90.0 - abs(observer.latitude - solar.declination)

  private val observer: Coordinates
  private val solar: SolarCoordinates
  private val prevSolar: SolarCoordinates
  private val nextSolar: SolarCoordinates
  private val approximateTransit: Double

  init {
    val julianDate = CalendricalHelper.julianDay(date.year, date.month, date.day)
    prevSolar = SolarCoordinates(julianDate - 1)
    solar = SolarCoordinates(julianDate)
    nextSolar = SolarCoordinates(julianDate + 1)
    approximateTransit = approximateTransit(
      coordinates.longitude,
      solar.apparentSiderealTime, solar.rightAscension
    )
    val solarAltitude = -SUNRISE_DEPRESSION
    observer = coordinates
    transit = correctedTransit(
      approximateTransit, coordinates.longitude,
      solar.apparentSiderealTime, solar.rightAscension, prevSolar.rightAscension,
      nextSolar.rightAscension
    )
    sunrise = correctedHourAngle(
      approximateTransit, solarAltitude,
      coordinates, false, solar.apparentSiderealTime, solar.rightAscension,
      prevSolar.rightAscension, nextSolar.rightAscension, solar.declination,
      prevSolar.declination, nextSolar.declination, interpolateDeclination
    )
    sunset = correctedHourAngle(
      approximateTransit, solarAltitude,
      coordinates, true, solar.apparentSiderealTime, solar.rightAscension,
      prevSolar.rightAscension, nextSolar.rightAscension, solar.declination,
      prevSolar.declination, nextSolar.declination, interpolateDeclination
    )
  }

  fun timeForSolarAngle(angle: Double, afterTransit: Boolean): Double {
    return correctedHourAngle(
      approximateTransit, angle, coordinates = observer,
      afterTransit, solar.apparentSiderealTime, solar.rightAscension,
      prevSolar.rightAscension, nextSolar.rightAscension, solar.declination,
      prevSolar.declination, nextSolar.declination, interpolateDeclination
    )
  }

  /**
   * How far apparent solar time runs ahead of mean solar time, in hours: the transit compared with
   * mean local noon. Diyanet's estimate below
   * [io.github.meypod.adhan_kotlin.internal.TakdirTable.MEAN_SOLAR_TIME_LATITUDE] does not include
   * it, which is what makes those published times symmetric about the solstice.
   */
  val equationOfTime: Double
    get() = transit - (12.0 - observer.longitude / 15.0)

  /**
   * The time at which the sun has covered [proportion] of its descent below the horizon for
   * this night, never going deeper than [angle].
   *
   * This is the bound behind [io.github.meypod.adhan_kotlin.HighLatitudeRule.PROPORTIONAL_DEPRESSION].
   * Returns NaN when the sun's nightly descent is too shallow to place the twilight below the
   * horizon at all, in which case the caller falls back to a night portion.
   */
  fun timeForProportionalDepression(
    angle: Double,
    proportion: Double,
    afterTransit: Boolean,
    inMeanSolarTime: Boolean = false
  ): Double {
    val unbounded = proportion * maximumDepression
    val bounded = minOf(angle, unbounded)
    if (bounded <= SUNRISE_DEPRESSION) return Double.NaN
    val time = timeForSolarAngle(-bounded, afterTransit)
    // The shift only applies where the estimate actually bites. Where it does not, the bound is
    // the true twilight and moving it would override times Diyanet publishes unestimated.
    return if (inMeanSolarTime && unbounded < angle) time - equationOfTime else time
  }

  // hours from transit
  fun afternoon(shadowLength: ShadowLength): Double {
    // TODO (from Swift version) source shadow angle calculation
    val tangent: Double = abs(observer.latitude - solar.declination)
    val inverse: Double =
      shadowLength.shadowLength + tan(tangent.toRadians())
    val angle: Double = atan(1.0 / inverse).toDegrees()
    return timeForSolarAngle(angle, true)
  }
}
