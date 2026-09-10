package io.github.meypod.adhan_kotlin

import io.github.meypod.adhan_kotlin.CalculationMethod.DUBAI
import io.github.meypod.adhan_kotlin.CalculationMethod.EGYPTIAN
import io.github.meypod.adhan_kotlin.CalculationMethod.KARACHI
import io.github.meypod.adhan_kotlin.CalculationMethod.KUWAIT
import io.github.meypod.adhan_kotlin.CalculationMethod.MOON_SIGHTING_COMMITTEE
import io.github.meypod.adhan_kotlin.CalculationMethod.MOROCCO
import io.github.meypod.adhan_kotlin.CalculationMethod.MUSLIM_WORLD_LEAGUE
import io.github.meypod.adhan_kotlin.CalculationMethod.NORTH_AMERICA
import io.github.meypod.adhan_kotlin.CalculationMethod.OTHER
import io.github.meypod.adhan_kotlin.CalculationMethod.QATAR
import io.github.meypod.adhan_kotlin.CalculationMethod.SINGAPORE
import io.github.meypod.adhan_kotlin.CalculationMethod.TURKEY
import io.github.meypod.adhan_kotlin.CalculationMethod.TURKEY_EUROPE
import io.github.meypod.adhan_kotlin.CalculationMethod.UMM_AL_QURA
import io.github.meypod.adhan_kotlin.HighLatitudeRule.MIDDLE_OF_THE_NIGHT
import io.github.meypod.adhan_kotlin.HighLatitudeRule.PROPORTIONAL_DEPRESSION
import io.github.meypod.adhan_kotlin.HighLatitudeRule.SEVENTH_OF_THE_NIGHT
import io.github.meypod.adhan_kotlin.HighLatitudeRule.TWILIGHT_ANGLE
import io.github.meypod.adhan_kotlin.Madhab.HANAFI
import io.github.meypod.adhan_kotlin.Madhab.SHAFI
import io.github.meypod.adhan_kotlin.data.DateComponents
import io.github.meypod.adhan_kotlin.data.TimingFile
import io.github.meypod.adhan_kotlin.data.TimingParameters
import io.github.meypod.adhan_kotlin.internal.TestUtils.getDateComponents
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.time.Instant

class TimingTest {

  @Test
  fun testTimes() {
    val json = Json { ignoreUnknownKeys = true }
    val testUtil = TestUtil()

    val root = (testUtil.environmentVariable("ADHAN_ROOT") ?: "").toPath()

    // disable time verification tests for platforms without filesystem support
    // currently, this is just wasm
    val fs = testUtil.fileSystem() ?: return

    val jsonPath = root.resolve("Shared/Times/")
    assertTrue(fs.exists(jsonPath), "Json Path Does not Exist: $jsonPath")

    val dir = fs.list(jsonPath)
    dir.forEach { path ->
      val byteString = fs.read(path) {
        readByteString()
      }
      val contents = byteString.utf8()
      val timingFile = json.decodeFromString<TimingFile>(contents)
      validateTimingFile(timingFile)
    }
  }

  private fun validateTimingFile(timingFile: TimingFile) {
    val coordinates = Coordinates(timingFile.params.latitude, timingFile.params.longitude)
    val parameters = parseParameters(timingFile.params)

    for ((date, fajr, sunrise, dhuhr, asr, maghrib, isha) in timingFile.times) {
      val dateComponents: DateComponents = getDateComponents(date)
      val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)
      val fajrDifference =
        getDifferenceInMinutes(prayerTimes.fajr, fajr, timingFile.params.timezone)
      assertTrue { fajrDifference <= timingFile.variance }
      val sunriseDifference =
        getDifferenceInMinutes(prayerTimes.sunrise, sunrise, timingFile.params.timezone)
      assertTrue { sunriseDifference <= timingFile.variance }
      val dhuhrDifference =
        getDifferenceInMinutes(prayerTimes.dhuhr, dhuhr, timingFile.params.timezone)
      assertTrue { dhuhrDifference <= timingFile.variance }
      val asrDifference = getDifferenceInMinutes(prayerTimes.asr, asr, timingFile.params.timezone)
      assertTrue { asrDifference <= timingFile.variance }
      val maghribDifference =
        getDifferenceInMinutes(prayerTimes.maghrib, maghrib, timingFile.params.timezone)
      assertTrue { maghribDifference <= timingFile.variance }
      val ishaDifference =
        getDifferenceInMinutes(prayerTimes.isha, isha, timingFile.params.timezone)
      assertTrue { ishaDifference <= timingFile.variance }
    }
  }

  private fun getDifferenceInMinutes(
    prayerTime: Instant,
    jsonTime: String,
    timezone: String
  ): Long {
    val (time, amPm) = jsonTime.split(" ")
    val (hours, minutes) = time.split(":").map { it.toInt() }
    val resolvedHour = when {
      amPm == "PM" && hours == 12 -> 12
      amPm == "PM" -> hours + 12
      amPm == "AM" && hours == 12 -> 0
      else -> hours
    }

    val targetTimeZone = TimeZone.of(timezone)
    val calculatedDateTime = prayerTime.toLocalDateTime(targetTimeZone)

    val referenceDateTime = LocalDateTime(
      // PrayerTimes is in UTC, so we need calculatedDateTime here instead
      calculatedDateTime.year,
      calculatedDateTime.month,
      calculatedDateTime.day,
      resolvedHour,
      minutes
    )

    val referenceInstant = referenceDateTime.toInstant(targetTimeZone)
    val calculatedInstant = calculatedDateTime.toInstant(targetTimeZone)

    return abs(calculatedInstant.toEpochMilliseconds() - referenceInstant.toEpochMilliseconds()) / (60 * 1000)
  }

  private fun parseParameters(timingParameters: TimingParameters): CalculationParameters {
    val method: CalculationMethod = when (timingParameters.method) {
      "MuslimWorldLeague" -> MUSLIM_WORLD_LEAGUE
      "Egyptian" -> EGYPTIAN
      "Karachi" -> KARACHI
      "UmmAlQura" -> UMM_AL_QURA
      "Dubai" -> DUBAI
      "MoonsightingCommittee" -> MOON_SIGHTING_COMMITTEE
      "Morocco" -> MOROCCO
      "NorthAmerica" -> NORTH_AMERICA
      "Kuwait" -> KUWAIT
      "Qatar" -> QATAR
      "Singapore" -> SINGAPORE
      "Turkey" -> TURKEY
      "TurkeyEurope" -> TURKEY_EUROPE
      else -> OTHER
    }

    val parameters = method.parameters
    val madhab = if ("Shafi" == timingParameters.madhab) {
      SHAFI
    } else if ("Hanafi" == timingParameters.madhab) {
      HANAFI
    } else {
      parameters.madhab
    }

    val highLatitudeRule = when (timingParameters.highLatitudeRule) {
      "SeventhOfTheNight" -> SEVENTH_OF_THE_NIGHT
      "TwilightAngle" -> TWILIGHT_ANGLE
      "ProportionalDepression" -> PROPORTIONAL_DEPRESSION
      else -> MIDDLE_OF_THE_NIGHT
    }
    return method.parameters.copy(madhab = madhab, highLatitudeRule = highLatitudeRule)
  }
}
