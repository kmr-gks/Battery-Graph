package com.gukos.battery_graph.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureUnitTest {

    @Test
    fun convertsTenthsOfCelsius() {
        assertEquals(25.0, TemperatureUnit.CELSIUS.fromTenthsCelsius(250), 0.001)
        assertEquals(77.0, TemperatureUnit.FAHRENHEIT.fromTenthsCelsius(250), 0.001)
        assertEquals(32.0, TemperatureUnit.FAHRENHEIT.fromTenthsCelsius(0), 0.001)
    }

    @Test
    fun formatsWithSymbol() {
        assertEquals("25.3℃", TemperatureUnit.CELSIUS.format(253))
        assertEquals("78°F", TemperatureUnit.FAHRENHEIT.format(253, decimals = 0))
    }

    @Test
    fun defaultDependsOnCountry() {
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.defaultFor("US"))
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.defaultFor("us"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.defaultFor("JP"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.defaultFor(""))
    }
}
