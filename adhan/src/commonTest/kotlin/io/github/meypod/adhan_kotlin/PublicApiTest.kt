package io.github.meypod.adhan_kotlin

import io.github.meypod.adhan_kotlin.data.DateComponents
import io.github.meypod.adhan_kotlin.internal.SolarTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Guards the published surface. Each of these broke once by accident: a `private companion object`
 * hid the generated serializer, and trailing defaulted parameters silently removed the shorter
 * overloads from the JVM ABI, which breaks consumers compiled against an earlier release.
 */
class PublicApiTest {

  @Test
  fun calculationParametersSerializerIsReachable() {
    // named explicitly, as a contextual/polymorphic SerializersModule or a wrapper would
    assertNotNull(ListSerializer(CalculationParameters.serializer()))
    val params = CalculationMethod.TURKEY_EUROPE.parameters
    val json = Json.encodeToString(CalculationParameters.serializer(), params)
    assertEquals(params, Json.decodeFromString(CalculationParameters.serializer(), json))
  }

  @Test
  fun preExistingOverloadsStillResolve() {
    val date = DateComponents(2026, 6, 21)
    val coordinates = Coordinates(52.5, 13.4)

    // the three-argument form predates interpolateDeclination and must keep working.
    // PolarResolvedValues holds SolarTime, which has reference equality, so compare observables.
    val three = resolvePolarCircleValues(PolarCircleResolution.AqrabBalad, date, coordinates)
    val four = resolvePolarCircleValues(PolarCircleResolution.AqrabBalad, date, coordinates, true)
    assertEquals(four.coordinates, three.coordinates)
    assertEquals(four.date, three.date)
    assertEquals(four.solarTime.transit, three.solarTime.transit)
    assertEquals(four.solarTime.sunrise, three.solarTime.sunrise)

    // likewise the two-argument SolarTime constructor
    assertEquals(SolarTime(date, coordinates, true).transit, SolarTime(date, coordinates).transit)
  }
}
