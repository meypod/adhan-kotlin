package io.github.meypod.adhan_kotlin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class DateUtilsTest {

  private fun t(offsetMs: Long) =
    Instant.fromEpochMilliseconds(1_000_000_000_000L + offsetMs).toLocalDateTime(TimeZone.UTC)

  @Test
  fun timesInOrder_after_returnsTrue() {
    assertTrue(PrayerTimes.timesInOrder(t(0), t(1)))
  }

  @Test
  fun timesInOrder_shortGapNearPolarCircle_returnsTrue() {
    assertTrue(PrayerTimes.timesInOrder(t(0), t(2 * 60 * 1000L)))
  }

  @Test
  fun timesInOrder_equal_returnsFalse() {
    assertFalse(PrayerTimes.timesInOrder(t(0), t(0)))
  }

  @Test
  fun timesInOrder_reversed_returnsFalse() {
    assertFalse(PrayerTimes.timesInOrder(t(1), t(0)))
  }

  @Test
  fun timesInOrder_firstNull_returnsFalse() {
    assertFalse(PrayerTimes.timesInOrder(null, t(0)))
  }

  @Test
  fun timesInOrder_secondNull_returnsFalse() {
    assertFalse(PrayerTimes.timesInOrder(t(0), null))
  }

  @Test
  fun timesInOrder_bothNull_returnsFalse() {
    assertFalse(PrayerTimes.timesInOrder(null, null))
  }
}
