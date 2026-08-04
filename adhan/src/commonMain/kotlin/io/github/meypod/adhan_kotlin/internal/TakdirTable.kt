package io.github.meypod.adhan_kotlin.internal

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The proportions behind Diyanet's high latitude "takdir"
 * ([io.github.meypod.adhan_kotlin.HighLatitudeRule.PROPORTIONAL_DEPRESSION]).
 *
 * Diyanet does not publish the formula it uses to estimate Imsak and Yatsı where the 18°/16°
 * twilight signs either never occur or occur impractically far from sunrise and sunset. What its
 * calendars do show is that the estimated times are symmetric about the solstice — so the rule is
 * driven by the sun's declination, not by the calendar — and that the twilight angle is bounded by
 * a proportion of how far the sun descends below the horizon that night. That proportion is
 * constant through the year for any one place, but varies with latitude, which is what this table
 * captures.
 *
 * Values are a least-error fit against Diyanet's published 2026 calendars for 61 European
 * locations between 40.9°N and 59.3°N — 22,265 reference times. Between the tabulated latitudes the
 * proportion is interpolated linearly; outside them the nearest end is held.
 *
 * Accuracy against those calendars: mean error 0.9 minutes for Fajr and 1.7 for Isha, 99th
 * percentile 6 minutes, worst case 9. That worst case is close to the floor for any formula of
 * this kind: Diyanet's own calendars disagree with each other by up to 7 minutes between locations
 * that share a latitude and a day length (Frankfurt and Prague, for instance), so part of their
 * estimate does not follow from geometry at all.
 */
internal object TakdirTable {
  /**
   * Diyanet estimates from this latitude upwards; south of it its calendars publish the plain
   * twilight angles, so no bound is applied. 44.5° is Diyanet's own published figure — "takdiri
   * vakitlerin uygulanmaya başlayacağı enlem sınırının 44,5 derece olduğu tespit edilmiştir" — and
   * it is corroborated by the calendars themselves: Belgrade at 44.79° and Bordeaux at 44.84° are
   * estimated, while Nice at 43.80° is not.
   */
  const val MIN_LATITUDE: Double = 44.5

  /**
   * Below this latitude the estimate is a fixed twilight duration; above it, a proportion of the
   * sun's nightly descent. The two forms agree closely either side of the crossover.
   */
  const val MAX_DURATION_LATITUDE: Double = 46.0

  /** Minutes before sunrise that Fajr is bounded at, at [MIN_LATITUDE] and [MAX_DURATION_LATITUDE]. */
  private const val FAJR_CAP_LOW = 134.0
  private const val FAJR_CAP_HIGH = 127.5

  /** Minutes after sunset that Isha is bounded at, at [MIN_LATITUDE] and [MAX_DURATION_LATITUDE]. */
  private const val ISHA_CAP_LOW = 118.0
  private const val ISHA_CAP_HIGH = 112.5

  /** Minutes before sunrise at which Fajr is bounded, for latitudes below [MAX_DURATION_LATITUDE]. */
  fun fajrDurationCap(latitude: Double): Int = durationCap(FAJR_CAP_LOW, FAJR_CAP_HIGH, latitude)

  /** Minutes after sunset at which Isha is bounded, for latitudes below [MAX_DURATION_LATITUDE]. */
  fun ishaDurationCap(latitude: Double): Int = durationCap(ISHA_CAP_LOW, ISHA_CAP_HIGH, latitude)

  private fun durationCap(low: Double, high: Double, latitude: Double): Int {
    val t = ((abs(latitude) - MIN_LATITUDE) / (MAX_DURATION_LATITUDE - MIN_LATITUDE))
      .coerceIn(0.0, 1.0)
    return (low + t * (high - low)).roundToInt()
  }

  /**
   * Where the bound stops being placed in mean solar time and becomes an ordinary apparent time.
   * This is 90° − 18° − 23.44°: the latitude at which the sun first fails to reach 18° below the
   * horizon at the summer solstice. Below it Diyanet is compressing a twilight that does exist and
   * places the result in mean solar time — the equation of time is not applied, which is worth up
   * to ten minutes around the solstice. Above it there is nothing to compress and the time is
   * constructed from geometry in ordinary apparent time.
   */
  const val MEAN_SOLAR_TIME_LATITUDE: Double = 48.56

  /**
   * Diyanet floors both the day and the night at five hours. Where the floor binds, sunrise and
   * maghrib are placed symmetrically about Öğle — 2h30m either side when the day is floored, and
   * 9h30m either side when the night is floored, which is the same thing measured around solar
   * midnight. Verified exactly, to the minute, on every floored day at Tromsø, Oulu, Trondheim,
   * Umeå, Helsinki, Tampere and Bergen.
   */
  const val MINIMUM_DAY_MINUTES: Int = 300

  /** Half of [MINIMUM_DAY_MINUTES]: sunrise and maghrib sit this far either side of Öğle. */
  const val FLOORED_DAY_HALF_MINUTES: Int = MINIMUM_DAY_MINUTES / 2

  /** Half of the floored night, measured from Öğle rather than from solar midnight. */
  const val FLOORED_NIGHT_HALF_MINUTES: Int = 720 - MINIMUM_DAY_MINUTES / 2

  /** Whether the bound at this latitude is placed in mean rather than apparent solar time. */
  fun usesMeanSolarTime(latitude: Double): Boolean = abs(latitude) < MEAN_SOLAR_TIME_LATITUDE

  fun fajrProportion(latitude: Double): Double =
    if (usesMeanSolarTime(latitude)) interpolate(MEAN_LATITUDES, MEAN_FAJR, latitude)
    else interpolate(APPARENT_LATITUDES, APPARENT_FAJR, latitude)

  fun ishaProportion(latitude: Double): Double =
    if (usesMeanSolarTime(latitude)) interpolate(MEAN_LATITUDES, MEAN_ISHA, latitude)
    else interpolate(APPARENT_LATITUDES, APPARENT_ISHA, latitude)

  private val MEAN_LATITUDES = doubleArrayOf(46.0, 46.5, 47.0, 47.5, 48.0, 48.56)
  private val MEAN_FAJR = doubleArrayOf(0.715, 0.708, 0.700, 0.689, 0.671, 0.637)
  private val MEAN_ISHA = doubleArrayOf(0.673, 0.660, 0.657, 0.646, 0.630, 0.601)

  private val APPARENT_LATITUDES = doubleArrayOf(48.56, 49.5, 51.0, 53.0, 56.0, 60.0)
  private val APPARENT_FAJR = doubleArrayOf(0.634, 0.644, 0.659, 0.661, 0.674, 0.688)
  private val APPARENT_ISHA = doubleArrayOf(0.585, 0.591, 0.609, 0.611, 0.625, 0.634)

  private fun interpolate(latitudes: DoubleArray, table: DoubleArray, latitude: Double): Double {
    val φ = abs(latitude)
    if (φ <= latitudes.first()) return table.first()
    if (φ >= latitudes.last()) return table.last()
    val upper = latitudes.indexOfFirst { it >= φ }
    val lower = upper - 1
    val t = (φ - latitudes[lower]) / (latitudes[upper] - latitudes[lower])
    return table[lower] + t * (table[upper] - table[lower])
  }
}
