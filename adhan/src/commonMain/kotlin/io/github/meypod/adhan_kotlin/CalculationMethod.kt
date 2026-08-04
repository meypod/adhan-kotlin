package io.github.meypod.adhan_kotlin

import io.github.meypod.adhan_kotlin.model.Rounding
import kotlinx.serialization.Serializable

/**
 * Standard calculation methods for calculating prayer times
 */
@Serializable
enum class CalculationMethod {
  /**
   * The default value for [CalculationParameters.method] when initializing a
   * [CalculationParameters] object. Sets a Fajr angle of 0 and an Isha angle of 0.
   */
  OTHER,

  /**
   * Moonsighting Committee
   * Uses a Fajr angle of 18 and an Isha angle of 18. Also uses seasonal adjustment values.
   */
  MOON_SIGHTING_COMMITTEE,

  /**
   * Muslim World League
   * Uses Fajr angle of 18 and an Isha angle of 17
   */
  MUSLIM_WORLD_LEAGUE,

  /**
   * Egyptian General Authority of Survey
   * Uses Fajr angle of 19.5 and an Isha angle of 17.5
   */
  EGYPTIAN,

  /**
   * University of Islamic Sciences, Karachi
   * Uses Fajr angle of 18 and an Isha angle of 18
   */
  KARACHI,

  /**
   * Umm al-Qura University, Makkah
   * Uses a Fajr angle of 18.5 and an Isha angle of 90. Note: You should add a +30 minute custom
   * adjustment of Isha during Ramadan.
   */
  UMM_AL_QURA,

  /**
   * Referred to as the ISNA method
   * This method is included for completeness, but is not recommended.
   * Uses a Fajr angle of 15 and an Isha angle of 15.
   */
  NORTH_AMERICA,

  /**
   * Gulf region
   * Modified version of Umm al-Qura that uses a Fajr angle of 19.5.
   */
  GULF,

  /**
   * The Gulf Region (Dubai)
   * Uses Fajr and Isha angles of 18.2 degrees.
   */
  DUBAI,

  /**
   * Kuwait
   * Uses a Fajr angle of 18 and an Isha angle of 17.5
   */
  KUWAIT,

  /**
   * Qatar
   * Modified version of Umm al-Qura that uses a Fajr angle of 18.
   */
  QATAR,

  /**
   * Singapore
   * Uses a Fajr angle of 20 and an Isha angle of 18
   */
  SINGAPORE,

  /**
   * Union Organization Islamic de France
   * Uses a Fajr angle of 12 and an Isha angle of 12.
   */
  FRANCE,

  /**
   * France angle 15
   * Uses a Fajr angle of 15 and an Isha angle of 15.
   */
  FRANCE15,

  /**
   * France angle 18
   * Uses a Fajr angle of 18 and an Isha angle of 18.
   */
  FRANCE18,

  /**
   * Diyanet İşleri Başkanlığı, Turkey
   * Uses a Fajr angle of 18 and an Isha angle of 17, Diyanet's 7 minute "temkin" on sunrise and
   * maghrib, and its high latitude "takdir" ([HighLatitudeRule.PROPORTIONAL_DEPRESSION]).
   *
   * This matches the calendars Diyanet publishes for Turkey and for the countries that follow
   * its 17° convention (among others Spain, Greece, Bulgaria, Albania, Azerbaijan, Egypt).
   * For Diyanet's European calendars use [TURKEY_EUROPE], which differs only in the Isha angle.
   */
  TURKEY,

  /**
   * Diyanet İşleri Başkanlığı, European calendars
   * Identical to [TURKEY] except that Isha uses an angle of 16 rather than 17.
   *
   * Diyanet publishes 16° for most of Europe — Germany, France, Italy, the Benelux, Austria,
   * Switzerland, the Nordics, the United Kingdom and the western Balkans among them — while
   * Turkey and a number of other countries keep 17°. Verify against the calendar for the
   * country in question if the difference (roughly 8 minutes) matters.
   */
  TURKEY_EUROPE,

  /**
   * Spiritual Administration of Muslims of Russia
   * Uses a Fajr angle of 16 and an Isha angle of 15.
   */
  RUSSIA,

  /**
   * Shia Ithna Ashari, Leva Institute, Qum, Iran
   * Uses Fajr angle of 16, Maghrib angle of 4 and Isha angle of 14
   */
  JAFARI,

  /**
   * Tehran
   * Uses a Fajr angle of 17.7 and an Isha angle of 14, and a maghrib angle of 4.5
   */
  TEHRAN,

  /**
   * Kementrian Agama Republik Indonesia (KEMENAG)
   * Uses Fajr angle of 20.0 and Isha angle of 18
   */
  KEMENAG,

  /**
   * Ministry of Religious Affairs and Wakfs, Algeria
   * Uses Fajr angle of 18, Isha angle of 17, + 3min maghrib
   */
  ALGERIA,

  /**
   * Kementrian Hal Ehwal Ugama (Brunei Darussalam)
   * Uses Fajr angle of 20 and Isha angle of 18
   */
  BRUNEI,

  /**
   * Ministry of Religious Affairs of Tunisia
   * Uses Fajr angle of 18 and Isha angle of 18
   */
  TUNISIA;

  /**
   * Return the CalculationParameters for the given method
   * @return CalculationParameters for the given Calculation method
   */
  val parameters: CalculationParameters
    get() = when (this) {
      OTHER -> {
        CalculationParameters(fajrAngle = 0.0, ishaAngle = 0.0, method = this)
      }

      MUSLIM_WORLD_LEAGUE -> {
        CalculationParameters(
            fajrAngle = 18.0, ishaAngle = 17.0, method = this,
            methodAdjustments = PrayerAdjustments(dhuhr = 1),
        )
      }

      MOON_SIGHTING_COMMITTEE -> {
        CalculationParameters(
            fajrAngle = 18.0, ishaAngle = 18.0, method = this,
            methodAdjustments = PrayerAdjustments(dhuhr = 5, sunset = 3, maghrib = 3),
        )
      }

      EGYPTIAN -> {
        CalculationParameters(
            fajrAngle = 19.5, ishaAngle = 17.5, method = this,
            methodAdjustments = PrayerAdjustments(dhuhr = 1),
        )
      }

      KARACHI -> {
        CalculationParameters(
            fajrAngle = 18.0, ishaAngle = 18.0, method = this,
            methodAdjustments = PrayerAdjustments(dhuhr = 1),
        )
      }

      UMM_AL_QURA -> {
        CalculationParameters(fajrAngle = 18.5, ishaInterval = 90, method = this)
      }

      NORTH_AMERICA -> {
        CalculationParameters(
            fajrAngle = 15.0, ishaAngle = 15.0, method = this,
            methodAdjustments = PrayerAdjustments(dhuhr = 1),
        )
      }

      GULF -> {
        CalculationParameters(fajrAngle = 19.5, ishaInterval = 90, method = this)
      }

      DUBAI -> {
        CalculationParameters(
            fajrAngle = 18.2, ishaAngle = 18.2, method = this,
            methodAdjustments = PrayerAdjustments(
                sunrise = -3,
                dhuhr = 3,
                asr = 3,
                sunset = 3,
                maghrib = 3,
            ),
        )
      }

      KUWAIT -> {
        CalculationParameters(fajrAngle = 18.0, ishaAngle = 17.5, method = this)
      }

      QATAR -> {
        CalculationParameters(fajrAngle = 18.0, ishaInterval = 90, method = this)
      }

      SINGAPORE -> {
        CalculationParameters(
            fajrAngle = 20.0, ishaAngle = 18.0, method = this,
            methodAdjustments = PrayerAdjustments(dhuhr = 1),
            rounding = Rounding.UP,
        )
      }

      FRANCE -> {
        CalculationParameters(fajrAngle = 12.0, ishaAngle = 12.0, method = this)
      }

      FRANCE15 -> {
        CalculationParameters(fajrAngle = 15.0, ishaAngle = 15.0, method = this)
      }

      FRANCE18 -> {
        CalculationParameters(fajrAngle = 18.0, ishaAngle = 18.0, method = this)
      }

      TURKEY, TURKEY_EUROPE -> {
        CalculationParameters(
            fajrAngle = 18.0,
            ishaAngle = if (this == TURKEY_EUROPE) 16.0 else 17.0,
            method = this,
            highLatitudeRule = HighLatitudeRule.PROPORTIONAL_DEPRESSION,
            // Diyanet holds the declination at 0h rather than interpolating it to each event
            interpolateDeclination = false,
            methodAdjustments = PrayerAdjustments(
                sunrise = -7,
                dhuhr = 5,
                asr = 4,
                sunset = 7,
                maghrib = 7,
            ),
        )
      }

      RUSSIA -> {
        CalculationParameters(fajrAngle = 16.0, ishaAngle = 15.0, method = this)
      }

      JAFARI -> {
        CalculationParameters(fajrAngle = 16.0, ishaAngle = 14.0, maghribAngle = 4.0, method = this)
      }

      TEHRAN -> {
        CalculationParameters(
            fajrAngle = 17.7, ishaAngle = 14.0,
            maghribAngle = 4.5, method = this,
        )
      }

      KEMENAG -> {
        CalculationParameters(fajrAngle = 20.0, ishaAngle = 18.0, method = this)
      }

      ALGERIA -> {
        CalculationParameters(
            fajrAngle = 18.0, ishaAngle = 17.0, method = this,
            methodAdjustments = PrayerAdjustments(
                sunset = 3,
                maghrib = 3,
            ),
        )
      }

      BRUNEI -> {
        CalculationParameters(fajrAngle = 20.0, ishaAngle = 18.0, method = this)
      }

      TUNISIA -> {
        CalculationParameters(fajrAngle = 18.0, ishaAngle = 18.0, method = this)
      }
    }
}
