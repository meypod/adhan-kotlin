package io.github.meypod.adhan_kotlin

import io.github.meypod.adhan_kotlin.model.Rounding
import io.github.meypod.adhan_kotlin.model.Shafaq
import kotlinx.serialization.Serializable

/**
 * Parameters used for PrayerTime calculation customization
 *
 * Note that, for many cases, you can use {@link CalculationMethod#getParameters()} to get a
 * pre-computed set of calculation parameters depending on one of the available
 * {@link CalculationMethod}.
 */
@Serializable
data class CalculationParameters(
  // The angle of the sun used to calculate fajr
  val fajrAngle: Double = 0.0,

  // The angle of the sun used to calculate isha
  val ishaAngle: Double = 0.0,

  // Minutes after Maghrib (if set, the time for Isha will be Maghrib plus IshaInterval)
  val ishaInterval: Int = 0,

  // Angle of the sun below the horizon used for calculating Maghrib.
  // Only used by the Tehran method to account for lightness in the sky.
  val maghribAngle: Double = 0.0,

  // The method used to do the calculation
  val method: CalculationMethod = CalculationMethod.OTHER,

  // The madhab used to calculate Asr
  val madhab: Madhab = Madhab.SHAFI,

  // Rules for placing bounds on Fajr and Isha for high latitude areas
  val highLatitudeRule: HighLatitudeRule? = null,

  // Used to optionally add or subtract a set amount of time from each prayer time
  val prayerAdjustments: PrayerAdjustments = PrayerAdjustments(),

  // Used for method adjustments
  val methodAdjustments: PrayerAdjustments = PrayerAdjustments(),

  // Rounding
  val rounding: Rounding = Rounding.NEAREST,

  // Twilight in the sky
  val shafaq: Shafaq = Shafaq.GENERAL,

  val polarCircleResolution: PolarCircleResolution = PolarCircleResolution.Unresolved,

  /**
   * Whether the sun's declination is interpolated to the moment of each event, as Astronomical
   * Algorithms prescribes, or held at its value for 0h universal time.
   *
   * Leave this `true` unless you are matching an authority that does not interpolate. Diyanet
   * appears not to: with the declination held at 0h its published sunrise, dhuhr, asr and maghrib
   * reproduce exactly, whereas interpolating moves the evening times by up to two minutes around
   * the equinoxes, in proportion to how fast the declination is changing.
   */
  val interpolateDeclination: Boolean = true
) {

  @Serializable
  data class NightPortions(val fajr: Double, val isha: Double)

  private companion object {
    /** See the PROPORTIONAL_DEPRESSION branch of [nightPortions]. */
    const val POLAR_NIGHT_PORTION_DIVISOR = 87.6
  }

  /**
   * The effective high latitude rule, resolving [highLatitudeRule] being unset to
   * [HighLatitudeRule.recommendedFor].
   */
  fun effectiveHighLatitudeRule(coordinates: Coordinates): HighLatitudeRule =
    highLatitudeRule ?: HighLatitudeRule.recommendedFor(coordinates)

  fun nightPortions(coordinates: Coordinates): NightPortions {
    return when (effectiveHighLatitudeRule(coordinates)) {
      HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> {
        NightPortions(1.0 / 2.0, 1.0 / 2.0)
      }
      HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> {
        NightPortions(1.0 / 7.0, 1.0 / 7.0)
      }
      HighLatitudeRule.TWILIGHT_ANGLE -> {
        NightPortions(this.fajrAngle / 60.0, this.ishaAngle / 60.0)
      }
      // PROPORTIONAL_DEPRESSION bounds the twilight angle rather than the night, so it has no
      // night portion of its own. This is only reached inside the polar circle, where the sun's
      // depression at solar midnight is too shallow for an angle to mean anything and Diyanet's
      // five-hour floor has fixed the night outright. Across every such day at Tromsø, Oulu,
      // Trondheim, Umeå, Helsinki, Tampere and Bergen, Diyanet places Fajr 0.2055 of that night
      // before sunrise and Isha 0.1829 after maghrib — the same divisor for both, since the two
      // scale as their angles do.
      HighLatitudeRule.PROPORTIONAL_DEPRESSION -> {
        NightPortions(
          this.fajrAngle / POLAR_NIGHT_PORTION_DIVISOR,
          this.ishaAngle / POLAR_NIGHT_PORTION_DIVISOR
        )
      }
    }
  }
}
