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
import io.github.meypod.adhan_kotlin.CalculationMethod.TURKEY
import io.github.meypod.adhan_kotlin.CalculationMethod.TURKEY_EUROPE
import io.github.meypod.adhan_kotlin.CalculationMethod.UMM_AL_QURA
import io.github.meypod.adhan_kotlin.HighLatitudeRule.PROPORTIONAL_DEPRESSION
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculationMethodTest {

  @Test
  fun testCalculationMethods() {
    var params = MUSLIM_WORLD_LEAGUE.parameters
    assertTrue { abs(params.fajrAngle - 18) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 17) <= 0.000001 }
    assertEquals(0, params.ishaInterval)
    assertEquals(MUSLIM_WORLD_LEAGUE, params.method)

    params = EGYPTIAN.parameters
    assertTrue { abs(params.fajrAngle - 19.5) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 17.5) <= 0.000001 }
    assertEquals(0, params.ishaInterval)
    assertEquals(EGYPTIAN, params.method)

    params = KARACHI.parameters
    assertTrue { abs(params.fajrAngle - 18) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 18) <= 0.000001 }
    assertEquals(0, params.ishaInterval)
    assertEquals(KARACHI, params.method)

    params = UMM_AL_QURA.parameters
    assertTrue { abs(params.fajrAngle - 18.5) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 0) <= 0.000001 }
    assertEquals(90, params.ishaInterval)
    assertEquals(UMM_AL_QURA, params.method)

    params = DUBAI.parameters
    assertTrue { abs(params.fajrAngle - 18.2) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 18.2) <= 0.000001 }
    assertEquals(0, params.ishaInterval)
    assertEquals(DUBAI, params.method)

    params = MOON_SIGHTING_COMMITTEE.parameters
    assertTrue { abs(params.fajrAngle - 18) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 18) <= 0.000001 }
    assertEquals(0, params.ishaInterval)
    assertEquals(MOON_SIGHTING_COMMITTEE, params.method)

    params = MOROCCO.parameters
    assertTrue { abs(params.fajrAngle - 19) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 17) <= 0.000001 }
    assertEquals(0, params.ishaInterval)
    assertEquals(MOROCCO, params.method)

    params = NORTH_AMERICA.parameters
    assertTrue { abs(params.fajrAngle - 15) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 15) <= 0.000001 }
    assertEquals(0, params.ishaInterval)
    assertEquals(NORTH_AMERICA, params.method)

    params = KUWAIT.parameters
    assertTrue { abs(params.fajrAngle - 18) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 17.5) <= 0.000001 }
    assertEquals(0, params.ishaInterval)
    assertEquals(KUWAIT, params.method)

    params = QATAR.parameters
    assertTrue { abs(params.fajrAngle - 18) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 0) <= 0.000001 }
    assertEquals(90, params.ishaInterval)
    assertEquals(QATAR, params.method)

    params = TURKEY.parameters
    assertTrue { abs(params.fajrAngle - 18) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 17) <= 0.000001 }
    assertEquals(PROPORTIONAL_DEPRESSION, params.highLatitudeRule)
    assertEquals(TURKEY, params.method)

    params = TURKEY_EUROPE.parameters
    assertTrue { abs(params.fajrAngle - 18) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 16) <= 0.000001 }
    assertEquals(PROPORTIONAL_DEPRESSION, params.highLatitudeRule)
    assertEquals(TURKEY.parameters.methodAdjustments, params.methodAdjustments)
    assertEquals(TURKEY_EUROPE, params.method)

    params = OTHER.parameters
    assertTrue { abs(params.fajrAngle - 0) <= 0.000001 }
    assertTrue { abs(params.ishaAngle - 0) <= 0.000001 }
    assertEquals(0, params.ishaInterval)
    assertEquals(OTHER, params.method)
  }
}
