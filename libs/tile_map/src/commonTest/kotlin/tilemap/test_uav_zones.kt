package de.drick.tilemap

import de.drick.compose.tilemap.decodeUavZones
import kotlinx.coroutines.test.runTest
import wtfflightlog.libs.tile_map.generated.resources.Res
import kotlin.test.Test

class UAVZonesParserTestCommon {

    @Test
    fun loadUAVZonesPortugal() = runTest {
        val dataString = loadFile("mapa_UASZoneVersion.js")
        val decoded = decodeUavZones(dataString)
        println(decoded)
    }

    private suspend fun loadFile(fileName: String): String {
        return Res.readBytes("files/$fileName").decodeToString()
    }
}