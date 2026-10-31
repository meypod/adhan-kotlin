package io.github.meypod.adhan_kotlin

import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.time.Instant

class PolarCircleResolutionTest {
  private val prayersToCheck = listOf("fajr", "sunrise", "maghrib", "isha")

  private val regularDate = DateComponents(2020, 5, 15) // May 15, 2020
  private val dateAffectedByPolarNight = DateComponents(2020, 12, 21) // Dec 21, 2020
  private val dateAffectedByMidnightSun = DateComponents(2020, 6, 21) // Jun 21, 2020

  private val regularCoordinates = Coordinates(31.947351, 35.227163)
  private val ArjeplogSweden = Coordinates(66.7222444, 17.7189)
  private val AmundsenScottAntarctic = Coordinates(-84.996, 0.01013)

  @Test
  fun `regular computation unresolved should fail on polar date`() {
    val params =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.Unresolved)

    // Expect failure because unresolved and location affected by midnight sun
    assertFailsWith<IllegalStateException> {
      PrayerTimes(ArjeplogSweden, dateAffectedByMidnightSun, params)
    }
  }

  @Test
  fun `regular computation with resolvers on normal date succeeds`() {
    val aqrabBalad =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.AqrabBalad)
    val aqrabYaum =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.AqrabYaum)

    PrayerTimes(ArjeplogSweden, regularDate, aqrabBalad)
    PrayerTimes(ArjeplogSweden, regularDate, aqrabYaum)
  }

  @Test
  fun `location outside polar circle does not require resolution`() {
    val aqrabBalad =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.AqrabBalad)
    val aqrabYaum =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.AqrabYaum)

    // Should not throw for regular coordinates even when date falls in polar-night range
    PrayerTimes(regularCoordinates, dateAffectedByPolarNight, aqrabBalad)
    PrayerTimes(regularCoordinates, dateAffectedByPolarNight, aqrabYaum)
  }

  @Test
  fun `midnight sun unresolved fails but resolvers succeed`() {
    val unresolved =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.Unresolved)
    val aqrabBalad =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.AqrabBalad)
    val aqrabYaum =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.AqrabYaum)

    assertFailsWith<IllegalStateException> {
      PrayerTimes(ArjeplogSweden, dateAffectedByMidnightSun, unresolved)
    }

    val p1 = PrayerTimes(ArjeplogSweden, dateAffectedByMidnightSun, aqrabBalad)
    val p2 = PrayerTimes(ArjeplogSweden, dateAffectedByMidnightSun, aqrabYaum)

    // ensure key prayers are present
    assertNotNull(p1.fajr)
    assertNotNull(p1.sunrise)
    assertNotNull(p1.maghrib)
    assertNotNull(p1.isha)

    assertNotNull(p2.fajr)
    assertNotNull(p2.sunrise)
    assertNotNull(p2.maghrib)
    assertNotNull(p2.isha)
  }

  @Test
  fun `polar night unresolved fails but resolvers succeed`() {
    val unresolved =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.Unresolved)
    val aqrabBalad =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.AqrabBalad)
    val aqrabYaum =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(polarCircleResolution = PolarCircleResolution.AqrabYaum)

    assertFailsWith<IllegalStateException> {
      PrayerTimes(AmundsenScottAntarctic, dateAffectedByPolarNight, unresolved)
    }

    val p1 = PrayerTimes(AmundsenScottAntarctic, dateAffectedByPolarNight, aqrabBalad)
    val p2 = PrayerTimes(AmundsenScottAntarctic, dateAffectedByPolarNight, aqrabYaum)

    assertNotNull(p1.fajr)
    assertNotNull(p1.sunrise)
    assertNotNull(p1.maghrib)
    assertNotNull(p1.isha)

    assertNotNull(p2.fajr)
    assertNotNull(p2.sunrise)
    assertNotNull(p2.maghrib)
    assertNotNull(p2.isha)
  }

  @Test
  fun `calculating times for the polar circle Stockholm values match expected`() {
    val coordinates = Coordinates(66.7222444, 17.7189)
    var params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
    params = params.copy(
        polarCircleResolution = PolarCircleResolution.AqrabYaum,
        highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT,
    )
    val date = DateComponents.from(Instant.fromEpochMilliseconds(1592697600000)) // 21 june 2020

    val p = PrayerTimes(coordinates, date, params)

    val tz = TimeZone.of("Europe/Stockholm")

    fun toLocalDateTimeInTz(instant: Instant): LocalDateTime {
      val dt = instant.toLocalDateTime(tz)
      return LocalDateTime(dt.year, dt.month, dt.day, dt.hour, dt.minute)
    }

    // Resolved to 8 July. Its sunset needs the iterated Δm correction: a single step put it at
    // 21:36 UTC, before 9 July's 22:18 while the days are shortening.
    assertEquals(LocalDateTime(2020, 6, 21, 0, 48), toLocalDateTimeInTz(p.fajr))
    assertEquals(LocalDateTime(2020, 6, 21, 0, 54), toLocalDateTimeInTz(p.sunrise))
    assertEquals(LocalDateTime(2020, 6, 21, 12, 55), toLocalDateTimeInTz(p.dhuhr))
    assertEquals(LocalDateTime(2020, 6, 21, 17, 49), toLocalDateTimeInTz(p.asr))
    assertEquals(LocalDateTime(2020, 6, 22, 0, 30), toLocalDateTimeInTz(p.maghrib))
    assertEquals(LocalDateTime(2020, 6, 22, 0, 37), toLocalDateTimeInTz(p.isha))
  }
}
