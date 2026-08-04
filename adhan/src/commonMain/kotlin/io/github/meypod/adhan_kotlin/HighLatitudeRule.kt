package io.github.meypod.adhan_kotlin

import kotlinx.serialization.Serializable

/**
 * Rules for dealing with Fajr and Isha at places with high latitudes
 */
@Serializable
enum class HighLatitudeRule {
  /**
   * Fajr will never be earlier than the middle of the night, and Isha will never be later than
   * the middle of the night.
   */
  MIDDLE_OF_THE_NIGHT,

  /**
   * Fajr will never be earlier than the beginning of the last seventh of the night, and Isha will
   * never be later than the end of hte first seventh of the night.
   */
  SEVENTH_OF_THE_NIGHT,

  /**
   * Similar to [HighLatitudeRule.SEVENTH_OF_THE_NIGHT], but instead of 1/7th, the faction
   * of the night used is fajrAngle / 60 and ishaAngle/60.
   */
  TWILIGHT_ANGLE,

  /**
   * The "takdir" (estimation) that Diyanet İşleri Başkanlığı applies from 44.5° latitude
   * onwards, where the 18°/17°/16° twilight signs either never occur in summer or occur so far
   * from sunrise and sunset that they are impractical.
   *
   * Instead of bounding the twilight by a fraction of the night, the twilight *angle* itself is
   * bounded by a proportion of how far the sun descends below the horizon that night — its
   * depression at solar midnight. Fajr therefore never moves earlier, nor Isha later, than the
   * point at which the sun has covered that proportion of its nightly descent. The proportion
   * depends on latitude; see [io.github.meypod.adhan_kotlin.internal.TakdirTable], which also
   * documents how it was derived and how closely it tracks Diyanet's published calendars.
   *
   * South of [io.github.meypod.adhan_kotlin.internal.TakdirTable.MIN_LATITUDE] no bound is applied
   * at all, matching Diyanet's calendars for Rome, Marseille, Nice and everything below them.
   */
  PROPORTIONAL_DEPRESSION;

  companion object {
    fun recommendedFor(coordinates: Coordinates): HighLatitudeRule {
      return if (coordinates.latitude > 48.0) {
        HighLatitudeRule.SEVENTH_OF_THE_NIGHT
      } else {
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT
      }
    }
  }
}
