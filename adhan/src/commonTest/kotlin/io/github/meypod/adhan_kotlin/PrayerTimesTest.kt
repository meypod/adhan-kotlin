package io.github.meypod.adhan_kotlin

import io.github.meypod.adhan_kotlin.CalculationMethod.KARACHI
import io.github.meypod.adhan_kotlin.CalculationMethod.MOON_SIGHTING_COMMITTEE
import io.github.meypod.adhan_kotlin.CalculationMethod.MUSLIM_WORLD_LEAGUE
import io.github.meypod.adhan_kotlin.CalculationMethod.NORTH_AMERICA
import io.github.meypod.adhan_kotlin.HighLatitudeRule.MIDDLE_OF_THE_NIGHT
import io.github.meypod.adhan_kotlin.HighLatitudeRule.SEVENTH_OF_THE_NIGHT
import io.github.meypod.adhan_kotlin.HighLatitudeRule.TWILIGHT_ANGLE
import io.github.meypod.adhan_kotlin.Madhab.HANAFI
import io.github.meypod.adhan_kotlin.Madhab.SHAFI
import io.github.meypod.adhan_kotlin.Prayer.ASR
import io.github.meypod.adhan_kotlin.Prayer.DHUHR
import io.github.meypod.adhan_kotlin.Prayer.FAJR
import io.github.meypod.adhan_kotlin.Prayer.ISHA
import io.github.meypod.adhan_kotlin.Prayer.MAGHRIB
import io.github.meypod.adhan_kotlin.Prayer.NONE
import io.github.meypod.adhan_kotlin.Prayer.SUNRISE
import io.github.meypod.adhan_kotlin.data.DateComponents
import io.github.meypod.adhan_kotlin.internal.TestUtils.addSeconds
import io.github.meypod.adhan_kotlin.internal.TestUtils.makeDate
import io.github.meypod.adhan_kotlin.internal.TestUtils.pad
import io.github.meypod.adhan_kotlin.model.Shafaq
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.math.absoluteValue

class PrayerTimesTest {

  @Test
  fun testDaysSinceSolstice() {
    daysSinceSolsticeTest(11,  year = 2016,  month = 1,  day = 1, latitude = 1.0)
    daysSinceSolsticeTest(10,  year = 2015,  month = 12,  day = 31, latitude = 1.0)
    daysSinceSolsticeTest(10,  year = 2016,  month = 12,  day = 31, latitude = 1.0)
    daysSinceSolsticeTest(0,  year = 2016,  month = 12,  day = 21, latitude = 1.0)
    daysSinceSolsticeTest(1,  year = 2016,  month = 12,  day = 22, latitude = 1.0)
    daysSinceSolsticeTest(71,  year = 2016,  month = 3,  day = 1, latitude = 1.0)
    daysSinceSolsticeTest(70,  year = 2015,  month = 3,  day = 1, latitude = 1.0)
    daysSinceSolsticeTest(365,  year = 2016,  month = 12,  day = 20, latitude = 1.0)
    daysSinceSolsticeTest(364,  year = 2015,  month = 12,  day = 20, latitude = 1.0)
    daysSinceSolsticeTest(0,  year = 2015,  month = 6,  day = 21, latitude = -1.0)
    daysSinceSolsticeTest(0,  year = 2016,  month = 6,  day = 21, latitude = -1.0)
    daysSinceSolsticeTest(364,  year = 2015,  month = 6,  day = 20, latitude = -1.0)
    daysSinceSolsticeTest(365,  year = 2016,  month = 6,  day = 20, latitude = -1.0)
  }

  private fun daysSinceSolsticeTest(value: Int, year: Int, month: Int, day: Int, latitude: Double) {
    // For Northern Hemisphere start from December 21
    // (DYY=0 for December 21, and counting forward, DYY=11 for January 1 and so on).
    // For Southern Hemisphere start from June 21
    // (DYY=0 for June 21, and counting forward)
    val localDateTime = makeDate(year, month, day)
    val dayOfYear: Int = localDateTime.dayOfYear
    assertEquals(value, PrayerTimes.daysSinceSolstice(dayOfYear, localDateTime.year, latitude))
  }

  private fun stringifyAtTimezone(time: Instant, zoneId: String): String {
    val timeZone = TimeZone.of(zoneId)
    val localDateTime = time.toLocalDateTime(timeZone)

    // hour is 0-23
    val initialHour = localDateTime.hour
    val mappedHour = when {
      initialHour == 0 -> 12
      initialHour > 12 -> initialHour - 12
      else -> initialHour
    }
    val hour = pad(mappedHour)
    val minutes = pad(localDateTime.minute)
    val amPM = if (initialHour >= 12) "PM" else "AM"
    return "$hour:$minutes $amPM"
  }

  @Test
  fun testPrayerTimes() {
    val date = DateComponents(2015, 7, 12)
    val params = NORTH_AMERICA.parameters.copy(madhab = HANAFI)

    val coordinates = Coordinates(35.7750, -78.6336)
    val prayerTimes = PrayerTimes(coordinates, date, params)

    val zoneId = "America/New_York"
    assertEquals("04:42 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("06:08 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("01:21 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("06:22 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("08:32 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("09:57 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))
  }

  @Test
  fun testOffsets() {
    val date = DateComponents(2015, 12, 1)
    val coordinates = Coordinates(35.7750, -78.6336)

    val zoneId = "America/New_York"
    val parameters = MUSLIM_WORLD_LEAGUE.parameters

    var prayerTimes = PrayerTimes(coordinates, date, parameters)
    assertEquals("05:35 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("07:06 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:05 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("02:42 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("05:01 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("06:26 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))

    val params = parameters.copy(
      prayerAdjustments = parameters.prayerAdjustments.copy(
        fajr = 10,
        sunrise = 10,
        dhuhr = 10,
        asr = 10,
        maghrib = 10,
        isha = 10
      )
    )
    prayerTimes = PrayerTimes(coordinates, date, params)
    assertEquals("05:45 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("07:16 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:15 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("02:52 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("05:11 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("06:36 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))

    prayerTimes = PrayerTimes(coordinates, date,
      params.copy(prayerAdjustments = PrayerAdjustments()))
    assertEquals("05:35 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("07:06 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:05 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("02:42 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("05:01 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("06:26 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))
  }

  @Test
  fun testMoonsightingMethod() {
    val date = DateComponents(2016, 1, 31)
    val coordinates = Coordinates(35.7750, -78.6336)
    val prayerTimes = PrayerTimes(
      coordinates, date, MOON_SIGHTING_COMMITTEE.parameters
    )

    val zoneId = "America/New_York"
    assertEquals("05:48 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("07:16 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:33 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("03:20 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("05:43 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("07:05 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))
  }

  @Test
  fun testMoonsightingMethodHighLat() {
    // Values from http://www.moonsighting.com/pray.php
    val date = DateComponents(2016, 1, 1)
    val parameters = MOON_SIGHTING_COMMITTEE.parameters.copy(madhab = HANAFI)
    val coordinates = Coordinates(59.9094, 10.7349)

    val zoneId = "Europe/Oslo"
    val prayerTimes = PrayerTimes(coordinates, date, parameters)
    assertEquals("07:34 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("09:19 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:25 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("01:36 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("03:25 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("05:02 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))
  }

  @Test
  fun testDiyanet() {
    // values from https://namazvakitleri.diyanet.gov.tr/en-US/9541/prayer-time-for-istanbul
    val date = DateComponents(2020, 4, 16)
    val parameters = CalculationMethod.TURKEY.parameters
    val coordinates = Coordinates(41.005616, 28.976380)

    val zoneId = "Europe/Istanbul"
    val prayerTimes = PrayerTimes(coordinates, date, parameters)
    assertEquals("04:44 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("06:16 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("01:09 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("04:52 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("07:51 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId)) // Diyanet 7:52pm
    assertEquals("09:17 PM", stringifyAtTimezone(prayerTimes.isha, zoneId)) // Diyanet 9:18pm
  }

  /**
   * Reference values below are Diyanet's published 2026 calendars
   * (https://namazvakitleri.diyanet.gov.tr). Deviations, where they exist, are noted per line.
   */
  @Test
  fun testDiyanetTurkeyIstanbulSolstice() {
    val prayerTimes = PrayerTimes(
      Coordinates(41.0082, 28.9784), DateComponents(2026, 6, 21), CalculationMethod.TURKEY.parameters
    )
    val zoneId = "Europe/Istanbul"
    assertEquals("03:24 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("05:25 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("01:11 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("05:11 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("08:47 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("10:38 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))
  }

  /** Erzurum sits at ~1900m; Diyanet applies no elevation correction, and neither do we. */
  @Test
  fun testDiyanetTurkeyErzurumSolstice() {
    val prayerTimes = PrayerTimes(
      Coordinates(39.9086, 41.2769), DateComponents(2026, 6, 21), CalculationMethod.TURKEY.parameters
    )
    val zoneId = "Europe/Istanbul"
    assertEquals("02:43 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId)) // Diyanet 2:44am
    assertEquals("04:39 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId)) // Diyanet 4:40am
    assertEquals("12:22 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("04:19 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("07:54 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("09:41 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))
  }

  /** Rome is below 44.5°, so no takdir is applied and the 16° Isha stands alone. */
  @Test
  fun testDiyanetEuropeRomeSolstice() {
    val prayerTimes = PrayerTimes(
      Coordinates(41.9028, 12.4964), DateComponents(2026, 6, 21),
      CalculationMethod.TURKEY_EUROPE.parameters
    )
    val zoneId = "Europe/Rome"
    assertEquals("03:23 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("05:28 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("01:17 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("05:19 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("08:56 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("10:42 PM", stringifyAtTimezone(prayerTimes.isha, zoneId)) // Diyanet 10:41pm
  }

  /** At the solstice Berlin never reaches 18°/16°, so the takdir bound decides Fajr and Isha. */
  @Test
  fun testDiyanetEuropeBerlinSolstice() {
    val prayerTimes = PrayerTimes(
      Coordinates(52.5200, 13.4050), DateComponents(2026, 6, 21),
      CalculationMethod.TURKEY_EUROPE.parameters
    )
    val zoneId = "Europe/Berlin"
    assertEquals("03:13 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId)) // Diyanet 3:10am
    assertEquals("04:36 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("01:13 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("05:37 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("09:40 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("10:54 PM", stringifyAtTimezone(prayerTimes.isha, zoneId)) // Diyanet 10:57pm
  }

  /** In winter the angles are reachable, so the takdir must not bind at all. */
  @Test
  fun testDiyanetEuropeBerlinWinter() {
    val prayerTimes = PrayerTimes(
      Coordinates(52.5200, 13.4050), DateComponents(2026, 12, 21),
      CalculationMethod.TURKEY_EUROPE.parameters
    )
    val zoneId = "Europe/Berlin"
    assertEquals("06:07 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("08:08 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:09 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("01:43 PM", stringifyAtTimezone(prayerTimes.asr, zoneId)) // Diyanet 1:42pm
    assertEquals("04:01 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("05:48 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))
  }

  @Test
  fun testDiyanetEuropeOsloWinter() {
    val prayerTimes = PrayerTimes(
      Coordinates(59.9139, 10.7522), DateComponents(2026, 12, 21),
      CalculationMethod.TURKEY_EUROPE.parameters
    )
    val zoneId = "Europe/Oslo"
    assertEquals("06:32 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("09:11 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:20 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("01:11 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("03:19 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("05:41 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))
  }

  /**
   * The takdir keys off the sun's depression, which is symmetric about the equator, so it must
   * behave the same at mirrored latitudes and must not throw where the bound degenerates.
   */
  @Test
  fun testDiyanetTakdirAtExtremeLatitudes() {
    val parameters = CalculationMethod.TURKEY_EUROPE.parameters
      .copy(polarCircleResolution = PolarCircleResolution.AqrabBalad)
    listOf(2026 to 6, 2026 to 12).forEach { (year, month) ->
      listOf(50.0, 60.0, 66.0, 70.0, 80.0).forEach { latitude ->
        listOf(latitude, -latitude).forEach { signedLatitude ->
          val times = PrayerTimes(
            Coordinates(signedLatitude, 10.0), DateComponents(year, month, 21), parameters
          )
          assertTrue(times.fajr <= times.sunrise, "fajr after sunrise at $signedLatitude")
          assertTrue(times.maghrib <= times.isha, "isha before maghrib at $signedLatitude")
        }
      }
    }
    // June in the north mirrors December in the south. The two are not identical to the second:
    // the equation of time differs between the solstices, so allow a minute.
    val north = PrayerTimes(
      Coordinates(52.52, 13.405), DateComponents(2026, 6, 21), parameters
    )
    val south = PrayerTimes(
      Coordinates(-52.52, 13.405), DateComponents(2026, 12, 21), parameters
    )
    val northTwilight = north.sunrise - north.fajr
    val southTwilight = south.sunrise - south.fajr
    assertTrue(
      (northTwilight - southTwilight).absoluteValue <= 1.minutes,
      "mirrored takdir differs: $northTwilight vs $southTwilight"
    )
  }

  /**
   * Diyanet floors the day and the night at five hours, which defines sunrise and maghrib inside
   * the polar circle where the sun may not cross the horizon at all. Reference values from its
   * published 2026 calendar for Tromsø.
   */
  @Test
  fun testDiyanetFiveHourFloorTromso() {
    val coordinates = Coordinates(69.6492, 18.9553)
    val zoneId = "Europe/Oslo"
    val parameters = CalculationMethod.TURKEY_EUROPE.parameters

    // polar night: the day is floored, and asr falls back to dhuhr because the sun never rises
    val winter = PrayerTimes(coordinates, DateComponents(2026, 12, 21), parameters)
    assertEquals("06:28 AM", stringifyAtTimezone(winter.fajr, zoneId))
    assertEquals("09:17 AM", stringifyAtTimezone(winter.sunrise, zoneId))
    assertEquals("11:47 AM", stringifyAtTimezone(winter.dhuhr, zoneId))
    assertEquals("11:47 AM", stringifyAtTimezone(winter.asr, zoneId))
    assertEquals("02:17 PM", stringifyAtTimezone(winter.maghrib, zoneId))
    assertEquals("04:31 PM", stringifyAtTimezone(winter.isha, zoneId))
    // the floored day is exactly five hours
    assertEquals(5.hours, winter.maghrib - winter.sunrise)

    // polar day: the night is floored instead
    val summer = PrayerTimes(coordinates, DateComponents(2026, 6, 21), parameters)
    assertEquals("03:21 AM", stringifyAtTimezone(summer.sunrise, zoneId))
    assertEquals("12:51 PM", stringifyAtTimezone(summer.dhuhr, zoneId))
    assertEquals("06:02 PM", stringifyAtTimezone(summer.asr, zoneId))
    assertEquals("10:21 PM", stringifyAtTimezone(summer.maghrib, zoneId))
    assertEquals("02:23 AM", stringifyAtTimezone(summer.fajr, zoneId)) // Diyanet 2:19am
    assertEquals("11:11 PM", stringifyAtTimezone(summer.isha, zoneId)) // Diyanet 11:16pm
    assertEquals(19.hours, summer.maghrib - summer.sunrise)
  }

  /** Inside the polar circle the times must still be ordered, and must not throw. */
  @Test
  fun testDiyanetPolarOrdering() {
    val parameters = CalculationMethod.TURKEY_EUROPE.parameters
    listOf(60.0, 65.0, 69.65, 75.0, 80.0, 89.0).forEach { latitude ->
      listOf(latitude, -latitude).forEach { signed ->
        (1..12).forEach { month ->
          val t = PrayerTimes(
            Coordinates(signed, 18.9553), DateComponents(2026, month, 21), parameters
          )
          assertTrue(t.fajr <= t.sunrise, "fajr after sunrise at $signed month $month")
          assertTrue(t.sunrise <= t.dhuhr, "sunrise after dhuhr at $signed month $month")
          assertTrue(t.dhuhr <= t.asr, "dhuhr after asr at $signed month $month")
          assertTrue(t.asr <= t.maghrib, "asr after maghrib at $signed month $month")
          assertTrue(t.maghrib <= t.isha, "maghrib after isha at $signed month $month")
        }
      }
    }
  }

  /** The takdir must never fire below 44.5°, where Diyanet publishes the raw angles. */
  @Test
  fun testDiyanetTakdirDoesNotApplyBelow45() {
    val parameters = CalculationMethod.TURKEY_EUROPE.parameters
    val unbounded = parameters.copy(highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE)
    val date = DateComponents(2026, 6, 21)
    listOf(Coordinates(43.7102, 7.2620), Coordinates(41.9028, 12.4964)).forEach { coordinates ->
      assertEquals(
        PrayerTimes(coordinates, date, unbounded).fajr,
        PrayerTimes(coordinates, date, parameters).fajr
      )
      assertEquals(
        PrayerTimes(coordinates, date, unbounded).isha,
        PrayerTimes(coordinates, date, parameters).isha
      )
    }
  }

  @Test
  fun testEgyptian() {
    val date = DateComponents(2020, 1, 1)
    val parameters = CalculationMethod.EGYPTIAN.parameters
    val coordinates = Coordinates(latitude = 30.028703, longitude = 31.249528)

    val zoneId = "Africa/Cairo"
    val prayerTimes = PrayerTimes(coordinates, date, parameters)
    assertEquals("05:18 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("06:51 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("11:59 AM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("02:47 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("05:06 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("06:29 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))
  }

  @Test
  fun testTehranTimes20251228() {
    val date = DateComponents(2025, 12, 28)
    val params = CalculationMethod.TEHRAN.parameters
    val coordinates = Coordinates(35.68889, 51.38972)

    val zoneId = "Asia/Tehran"
    val prayerTimes = PrayerTimes(coordinates, date, params)
    val sunnah = SunnahTimes(prayerTimes)

    assertEquals("05:43 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("07:13 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:06 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("04:59 PM", stringifyAtTimezone(prayerTimes.sunset, zoneId))
    assertEquals("05:19 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("11:22 PM", stringifyAtTimezone(sunnah.middleOfTheNight, zoneId))
  }

  @Test
  fun testTimeForPrayer() {
    val components = DateComponents(2016, 7, 1)
    val parameters = MUSLIM_WORLD_LEAGUE.parameters.copy(
      madhab = HANAFI, highLatitudeRule = TWILIGHT_ANGLE)
    val coordinates = Coordinates(59.9094, 10.7349)

    val p = PrayerTimes(coordinates, components, parameters)
    assertEquals(p.fajr, p.timeForPrayer(FAJR))
    assertEquals(p.sunrise, p.timeForPrayer(SUNRISE))
    assertEquals(p.dhuhr, p.timeForPrayer(DHUHR))
    assertEquals(p.asr, p.timeForPrayer(ASR))
    assertEquals(p.maghrib, p.timeForPrayer(MAGHRIB))
    assertEquals(p.isha, p.timeForPrayer(ISHA))

    assertNull(p.timeForPrayer(NONE))
  }

  @Test
  fun testCurrentPrayer() {
    val components = DateComponents(2015, 9, 1)
    val parameters = KARACHI.parameters.copy(madhab = HANAFI, highLatitudeRule = TWILIGHT_ANGLE)
    val coordinates = Coordinates(33.720817, 73.090032)

    val p = PrayerTimes(coordinates, components, parameters)
    assertEquals(NONE, p.currentPrayer(addSeconds(p.fajr, -1)))
    assertEquals(FAJR, p.currentPrayer(p.fajr))
    assertEquals(FAJR, p.currentPrayer(addSeconds(p.fajr, 1)))
    assertEquals(SUNRISE, p.currentPrayer(addSeconds(p.sunrise, 1)))
    assertEquals(DHUHR, p.currentPrayer(addSeconds(p.dhuhr, 1)))
    assertEquals(ASR, p.currentPrayer(addSeconds(p.asr, 1)))
    assertEquals(MAGHRIB, p.currentPrayer(addSeconds(p.maghrib, 1)))
    assertEquals(ISHA, p.currentPrayer(addSeconds(p.isha, 1)))
  }

  @Test
  fun testNextPrayer() {
    val components = DateComponents(2015, 9, 1)
    val parameters = KARACHI.parameters.copy(madhab = HANAFI, highLatitudeRule = TWILIGHT_ANGLE)
    val coordinates = Coordinates(33.720817, 73.090032)

    val p = PrayerTimes(coordinates, components, parameters)
    assertEquals(FAJR, p.nextPrayer(addSeconds(p.fajr, -1)))
    assertEquals(SUNRISE, p.nextPrayer(p.fajr))
    assertEquals(SUNRISE, p.nextPrayer(addSeconds(p.fajr, 1)))
    assertEquals(DHUHR, p.nextPrayer(addSeconds(p.sunrise, 1)))
    assertEquals(ASR, p.nextPrayer(addSeconds(p.dhuhr, 1)))
    assertEquals(MAGHRIB, p.nextPrayer(addSeconds(p.asr, 1)))
    assertEquals(ISHA, p.nextPrayer(addSeconds(p.maghrib, 1)))
    assertEquals(NONE, p.nextPrayer(addSeconds(p.isha, 1)))
  }

  @Test
  fun testInvalidDate() {
    assertFailsWith<IllegalArgumentException> {
      val date = DateComponents(0, 0, 0)
      PrayerTimes(Coordinates(33.720817, 73.090032), date, MUSLIM_WORLD_LEAGUE.parameters)
    }

    assertFailsWith<IllegalArgumentException> {
      val date = DateComponents(-1, 99, 99)
      PrayerTimes(Coordinates(33.720817, 73.090032), date, MUSLIM_WORLD_LEAGUE.parameters)
    }
  }

  @Test
  fun testInvalidLocation() {
    assertFailsWith<IllegalArgumentException> {
      val date = DateComponents(2019, 1, 1)
      PrayerTimes(Coordinates(999.0, 999.0), date, MUSLIM_WORLD_LEAGUE.parameters)
    }
  }

  @Test
  fun testExtremeLocation() {
    assertFailsWith<IllegalStateException> {
      val date = DateComponents(2018, 1, 1)
      PrayerTimes(Coordinates(71.275009, -156.761368), date, MUSLIM_WORLD_LEAGUE.parameters)
    }

    val date = DateComponents(2018, 3, 1)
    val prayerTimes = PrayerTimes(Coordinates(71.275009, -156.761368), date, MUSLIM_WORLD_LEAGUE.parameters)
    assertNotNull(prayerTimes.fajr)
  }

  @Test
  fun testHighLatitudeRule() {
    val date = DateComponents(2020, 6, 15)
    val parameters = MUSLIM_WORLD_LEAGUE.parameters.copy(highLatitudeRule = MIDDLE_OF_THE_NIGHT)
    val coordinates = Coordinates(latitude = 55.983226, longitude = -3.216649)

    val zoneId = "Europe/London"
    val prayerTimes = PrayerTimes(coordinates, date, parameters)
    assertEquals("01:14 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("04:26 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("01:14 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("05:46 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("10:01 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("01:14 AM", stringifyAtTimezone(prayerTimes.isha, zoneId))

    val seventhParams = parameters.copy(highLatitudeRule = SEVENTH_OF_THE_NIGHT)
    val seventhPrayerTimes = PrayerTimes(coordinates, date, seventhParams)
    assertEquals("03:31 AM", stringifyAtTimezone(seventhPrayerTimes.fajr, zoneId))
    assertEquals("04:26 AM", stringifyAtTimezone(seventhPrayerTimes.sunrise, zoneId))
    assertEquals("01:14 PM", stringifyAtTimezone(seventhPrayerTimes.dhuhr, zoneId))
    assertEquals("05:46 PM", stringifyAtTimezone(seventhPrayerTimes.asr, zoneId))
    assertEquals("10:01 PM", stringifyAtTimezone(seventhPrayerTimes.maghrib, zoneId))
    assertEquals("10:56 PM", stringifyAtTimezone(seventhPrayerTimes.isha, zoneId))

    val twilightParams = parameters.copy(highLatitudeRule = TWILIGHT_ANGLE)
    val twilightPrayerTimes = PrayerTimes(coordinates, date, twilightParams)
    assertEquals("02:31 AM", stringifyAtTimezone(twilightPrayerTimes.fajr, zoneId))
    assertEquals("04:26 AM", stringifyAtTimezone(twilightPrayerTimes.sunrise, zoneId))
    assertEquals("01:14 PM", stringifyAtTimezone(twilightPrayerTimes.dhuhr, zoneId))
    assertEquals("05:46 PM", stringifyAtTimezone(twilightPrayerTimes.asr, zoneId))
    assertEquals("10:01 PM", stringifyAtTimezone(twilightPrayerTimes.maghrib, zoneId))
    assertEquals("11:50 PM", stringifyAtTimezone(twilightPrayerTimes.isha, zoneId))

    val autoHighLatitudeRule = parameters.copy(highLatitudeRule = null)
    val autoPrayerTimes = PrayerTimes(coordinates, date, autoHighLatitudeRule)
    assertEquals(seventhPrayerTimes.fajr, autoPrayerTimes.fajr)
    assertEquals(seventhPrayerTimes.sunrise, autoPrayerTimes.sunrise)
    assertEquals(seventhPrayerTimes.dhuhr, autoPrayerTimes.dhuhr)
    assertEquals(seventhPrayerTimes.asr, autoPrayerTimes.asr)
    assertEquals(seventhPrayerTimes.maghrib, autoPrayerTimes.maghrib)
    assertEquals(seventhPrayerTimes.isha, autoPrayerTimes.isha)
  }

  @Test
  fun testRecommendedHighLatitudeRule() {
    val coords1 = Coordinates(latitude = 45.983226, longitude = -3.216649)
    assertEquals(MIDDLE_OF_THE_NIGHT, HighLatitudeRule.recommendedFor(coords1))

    val coords2 = Coordinates(latitude = 48.983226, longitude = -3.216649)
    assertEquals(SEVENTH_OF_THE_NIGHT, HighLatitudeRule.recommendedFor(coords2))
  }

  @Test
  fun testShafaqGeneral() {
    val parameters = MOON_SIGHTING_COMMITTEE.parameters.copy(shafaq = Shafaq.GENERAL, madhab = HANAFI)
    val coordinates = Coordinates(latitude = 43.494, longitude = -79.844)

    val zoneId = "America/New_York"

    val date = DateComponents(2021, 1, 1)
    val prayerTimes = PrayerTimes(coordinates, date, parameters)
    assertEquals("06:16 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("07:52 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:28 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("03:12 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("04:57 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("06:27 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))

    val secondDate = DateComponents(2021, 4, 1)
    val secondPrayerTimes = PrayerTimes(coordinates, secondDate, parameters)
    assertEquals("05:28 AM", stringifyAtTimezone(secondPrayerTimes.fajr, zoneId))
    assertEquals("07:01 AM", stringifyAtTimezone(secondPrayerTimes.sunrise, zoneId))
    assertEquals("01:28 PM", stringifyAtTimezone(secondPrayerTimes.dhuhr, zoneId))
    assertEquals("05:53 PM", stringifyAtTimezone(secondPrayerTimes.asr, zoneId))
    assertEquals("07:49 PM", stringifyAtTimezone(secondPrayerTimes.maghrib, zoneId))
    assertEquals("09:01 PM", stringifyAtTimezone(secondPrayerTimes.isha, zoneId))

    val thirdDate = DateComponents(2021, 7, 1)
    val thirdPrayerTimes = PrayerTimes(coordinates, thirdDate, parameters)
    assertEquals("03:52 AM", stringifyAtTimezone(thirdPrayerTimes.fajr, zoneId))
    assertEquals("05:42 AM", stringifyAtTimezone(thirdPrayerTimes.sunrise, zoneId))
    assertEquals("01:28 PM", stringifyAtTimezone(thirdPrayerTimes.dhuhr, zoneId))
    assertEquals("06:42 PM", stringifyAtTimezone(thirdPrayerTimes.asr, zoneId))
    assertEquals("09:07 PM", stringifyAtTimezone(thirdPrayerTimes.maghrib, zoneId))
    assertEquals("10:22 PM", stringifyAtTimezone(thirdPrayerTimes.isha, zoneId))

    val fourthDate = DateComponents(2021, 11, 1)
    val fourthPrayerTimes = PrayerTimes(coordinates, fourthDate, parameters)
    assertEquals("06:22 AM", stringifyAtTimezone(fourthPrayerTimes.fajr, zoneId))
    assertEquals("07:55 AM", stringifyAtTimezone(fourthPrayerTimes.sunrise, zoneId))
    assertEquals("01:08 PM", stringifyAtTimezone(fourthPrayerTimes.dhuhr, zoneId))
    assertEquals("04:26 PM", stringifyAtTimezone(fourthPrayerTimes.asr, zoneId))
    assertEquals("06:13 PM", stringifyAtTimezone(fourthPrayerTimes.maghrib, zoneId))
    assertEquals("07:35 PM", stringifyAtTimezone(fourthPrayerTimes.isha, zoneId))
  }

  @Test
  fun testShafaqAhmer() {
    val parameters = MOON_SIGHTING_COMMITTEE.parameters.copy(shafaq = Shafaq.AHMER)
    val coordinates = Coordinates(latitude = 43.494, longitude = -79.844)

    val zoneId = "America/New_York"

    val date = DateComponents(2021, 1, 1)
    val prayerTimes = PrayerTimes(coordinates, date, parameters)
    assertEquals("06:16 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("07:52 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:28 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("02:37 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("04:57 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("06:07 PM", stringifyAtTimezone(prayerTimes.isha, zoneId)) // value from source is 6:08 PM

    val secondDate = DateComponents(2021, 4, 1)
    val secondPrayerTimes = PrayerTimes(coordinates, secondDate, parameters)
    assertEquals("05:28 AM", stringifyAtTimezone(secondPrayerTimes.fajr, zoneId))
    assertEquals("07:01 AM", stringifyAtTimezone(secondPrayerTimes.sunrise, zoneId))
    assertEquals("01:28 PM", stringifyAtTimezone(secondPrayerTimes.dhuhr, zoneId))
    assertEquals("04:59 PM", stringifyAtTimezone(secondPrayerTimes.asr, zoneId))
    assertEquals("07:49 PM", stringifyAtTimezone(secondPrayerTimes.maghrib, zoneId))
    assertEquals("08:45 PM", stringifyAtTimezone(secondPrayerTimes.isha, zoneId))

    val thirdDate = DateComponents(2021, 7, 1)
    val thirdPrayerTimes = PrayerTimes(coordinates, thirdDate, parameters)
    assertEquals("03:52 AM", stringifyAtTimezone(thirdPrayerTimes.fajr, zoneId))
    assertEquals("05:42 AM", stringifyAtTimezone(thirdPrayerTimes.sunrise, zoneId))
    assertEquals("01:28 PM", stringifyAtTimezone(thirdPrayerTimes.dhuhr, zoneId))
    assertEquals("05:29 PM", stringifyAtTimezone(thirdPrayerTimes.asr, zoneId))
    assertEquals("09:07 PM", stringifyAtTimezone(thirdPrayerTimes.maghrib, zoneId))
    assertEquals("10:19 PM", stringifyAtTimezone(thirdPrayerTimes.isha, zoneId))

    val fourthDate = DateComponents(2021, 11, 1)
    val fourthPrayerTimes = PrayerTimes(coordinates, fourthDate, parameters)
    assertEquals("06:22 AM", stringifyAtTimezone(fourthPrayerTimes.fajr, zoneId))
    assertEquals("07:55 AM", stringifyAtTimezone(fourthPrayerTimes.sunrise, zoneId))
    assertEquals("01:08 PM", stringifyAtTimezone(fourthPrayerTimes.dhuhr, zoneId))
    assertEquals("03:45 PM", stringifyAtTimezone(fourthPrayerTimes.asr, zoneId))
    assertEquals("06:13 PM", stringifyAtTimezone(fourthPrayerTimes.maghrib, zoneId))
    assertEquals("07:15 PM", stringifyAtTimezone(fourthPrayerTimes.isha, zoneId))
  }

  @Test
  fun testShafaqAbyad() {
    val parameters = MOON_SIGHTING_COMMITTEE.parameters.copy(shafaq = Shafaq.ABYAD, madhab = HANAFI)
    val coordinates = Coordinates(latitude = 43.494, longitude = -79.844)

    val zoneId = "America/New_York"

    val date = DateComponents(2021, 1, 1)
    val prayerTimes = PrayerTimes(coordinates, date, parameters)
    assertEquals("06:16 AM", stringifyAtTimezone(prayerTimes.fajr, zoneId))
    assertEquals("07:52 AM", stringifyAtTimezone(prayerTimes.sunrise, zoneId))
    assertEquals("12:28 PM", stringifyAtTimezone(prayerTimes.dhuhr, zoneId))
    assertEquals("03:12 PM", stringifyAtTimezone(prayerTimes.asr, zoneId))
    assertEquals("04:57 PM", stringifyAtTimezone(prayerTimes.maghrib, zoneId))
    assertEquals("06:28 PM", stringifyAtTimezone(prayerTimes.isha, zoneId))

    val secondDate = DateComponents(2021, 4, 1)
    val secondPrayerTimes = PrayerTimes(coordinates, secondDate, parameters)
    assertEquals("05:28 AM", stringifyAtTimezone(secondPrayerTimes.fajr, zoneId))
    assertEquals("07:01 AM", stringifyAtTimezone(secondPrayerTimes.sunrise, zoneId))
    assertEquals("01:28 PM", stringifyAtTimezone(secondPrayerTimes.dhuhr, zoneId))
    assertEquals("05:53 PM", stringifyAtTimezone(secondPrayerTimes.asr, zoneId))
    assertEquals("07:49 PM", stringifyAtTimezone(secondPrayerTimes.maghrib, zoneId))
    assertEquals("09:12 PM", stringifyAtTimezone(secondPrayerTimes.isha, zoneId))

    val thirdDate = DateComponents(2021, 7, 1)
    val thirdPrayerTimes = PrayerTimes(coordinates, thirdDate, parameters)
    assertEquals("03:52 AM", stringifyAtTimezone(thirdPrayerTimes.fajr, zoneId))
    assertEquals("05:42 AM", stringifyAtTimezone(thirdPrayerTimes.sunrise, zoneId))
    assertEquals("01:28 PM", stringifyAtTimezone(thirdPrayerTimes.dhuhr, zoneId))
    assertEquals("06:42 PM", stringifyAtTimezone(thirdPrayerTimes.asr, zoneId))
    assertEquals("09:07 PM", stringifyAtTimezone(thirdPrayerTimes.maghrib, zoneId))
    assertEquals("11:17 PM", stringifyAtTimezone(thirdPrayerTimes.isha, zoneId))

    val fourthDate = DateComponents(2021, 11, 1)
    val fourthPrayerTimes = PrayerTimes(coordinates, fourthDate, parameters)
    assertEquals("06:22 AM", stringifyAtTimezone(fourthPrayerTimes.fajr, zoneId))
    assertEquals("07:55 AM", stringifyAtTimezone(fourthPrayerTimes.sunrise, zoneId))
    assertEquals("01:08 PM", stringifyAtTimezone(fourthPrayerTimes.dhuhr, zoneId))
    assertEquals("04:26 PM", stringifyAtTimezone(fourthPrayerTimes.asr, zoneId))
    assertEquals("06:13 PM", stringifyAtTimezone(fourthPrayerTimes.maghrib, zoneId))
    assertEquals("07:37 PM", stringifyAtTimezone(fourthPrayerTimes.isha, zoneId))
  }

  @Test
  fun testPrayerTimesProblems_fajr_after_sunrise__and_isha_before_maghrib1() {
    checkPrayersOrder(DateComponents(2025, 12, 1), Coordinates(42.74674252600066, 177.2401196144623))
  }

  @Test
  fun testPrayerTimesProblems_fajr_after_sunrise__and_isha_before_maghrib2() {
    checkPrayersOrder(DateComponents(2025, 12, 1), Coordinates(47.082209457885355, 177.24642294208638))
  }

  private fun checkPrayersOrder(date: DateComponents, coordinates: Coordinates) {
    val params = MUSLIM_WORLD_LEAGUE.parameters.copy(
      madhab = SHAFI,
      highLatitudeRule = TWILIGHT_ANGLE)
    val prayerTimes = PrayerTimes(coordinates, date, params)
    assertTrue(prayerTimes.fajr.epochSeconds < prayerTimes.sunrise.epochSeconds)
    assertTrue(prayerTimes.sunrise.epochSeconds < prayerTimes.dhuhr.epochSeconds)
    assertTrue(prayerTimes.dhuhr.epochSeconds < prayerTimes.asr.epochSeconds)
    assertTrue(prayerTimes.asr.epochSeconds < prayerTimes.maghrib.epochSeconds)
    assertTrue(prayerTimes.maghrib.epochSeconds < prayerTimes.isha.epochSeconds)
  }
}
