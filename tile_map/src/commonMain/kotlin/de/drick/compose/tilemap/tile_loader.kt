package de.drick.compose.tilemap

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import de.drick.core.log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

const val debugModeEnabled = false

val tileProviderOsm = TileProvider(
    name = "Open Street Map",
    tileLoaderUrl = { pos ->
        URLBuilder("https://tile.openstreetmap.org").apply {
            appendPathSegments(pos.zoom.toString(), pos.tileX.toString(), "${pos.tileY}.png")
        }.build()
    }
)

private const val mapboxToken = BuildConfig.MAPBOX_TOKEN

val tileProviderMapBox = TileProvider(
    name = "MapBox",
    tileLoaderUrl = { pos ->
        URLBuilder("https://api.mapbox.com/styles/v1/mapbox/satellite-v9/tiles/512").apply {
            appendPathSegments(pos.zoom.toString(), pos.tileX.toString(), pos.tileY.toString())
            parameters.append("access_token", mapboxToken)
        }.build()
    }
)



private val client by lazy {
    HttpClient {
        if (debugModeEnabled) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        de.drick.core.log("HTTP Client $message")
                    }
                }
                level = LogLevel.HEADERS
            }
        }
    }
}

class TileProvider(
    val name: String,
    private val tileLoaderUrl: (TilePos) -> Url,
) {
    override fun toString() = name
    val inMemoryCache = mutableMapOf<TilePos, ByteArray>()
    suspend fun loadTile(pos: TilePos) = withContext(Dispatchers.Default) {
        val url = tileLoaderUrl(pos)
        val response = client.request(url)
        if (response.status.isSuccess()) {
            val bodyBytes = response.bodyAsBytes()
            try {
                val image = bodyBytes.decodeToImageBitmap()
                if (image.height > 0 && image.width > 0) {
                    inMemoryCache[pos] = bodyBytes
                    image
                } else {
                    log("$name invalid data: ${response.bodyAsText()}")
                    null
                }
            } catch (int: CancellationException) {
                throw int
            } catch (err: Throwable) {
                log("No valid image data:\n${response.bodyAsText()}", err)
                null
            }
        } else {
            log("$name: Failed to load tile from $url, status: ${response.status}")
            null
        }
    }
    fun cachedTile(pos: TilePos): ImageBitmap? =
        inMemoryCache[pos]?.decodeToImageBitmap()
}

val tileProviderDipul = TileProvider(
    name = "Dipul",
    tileLoaderUrl = { pos ->
        URLBuilder("https://sgx.geodatenzentrum.de/wms_topplus_open").apply {
            parameters.apply {
                append("REQUEST", "GetMap")
                append("SERVICE", "WMS")
                append("STYLES", "")
                append("VERSION", "1.3.0")
                append("FORMAT", "image/png")
                append("TRANSPARENT", "TRUE")
                append("LAYERS", "web_scale_grau")
                append("TILED", "true")
                append("WIDTH", "256")
                append("HEIGHT", "256")
                append("CRS", "EPSG:3857")
                with(calculateBoundingBox(pos)) {
                    append("BBOX", "$startX,$startY,$endX,$endY")
                }
            }
        }.build()
    }
)

val layer = listOf(
    "flugplaetze", "flughaefen", "kontrollzonen", "flugbeschraenkungsgebiete", "bundesautobahnen", "bundesstrassen",
    "bahnanlagen", "binnenwasserstrassen", "seewasserstrassen", "schifffahrtsanlagen", "wohngrundstuecke", "freibaeder",
    "industrieanlagen", "kraftwerke", "umspannwerke", "stromleitungen", "windkraftanlagen", "justizvollzugsanstalten",
    "militaerische_anlagen", "labore", "behoerden", "diplomatische_vertretungen", "internationale_organisationen",
    "polizei", "sicherheitsbehoerden", "krankenhaeuser", "nationalparks", "naturschutzgebiete", "vogelschutzgebiete",
    "ffh-gebiete", "temporaere_betriebseinschraenkungen", "modellflugplaetze", "haengegleiter"
).map { "dipul:$it" }


val tileProviderDipulZones = TileProvider(
    name = "Dipul Zones",
    tileLoaderUrl = { pos ->
        URLBuilder("https://uas-betrieb.de/geoservices/dipul/wms").apply {
            parameters.apply {
                append("REQUEST", "GetMap")
                append("SERVICE", "WMS")
                append("STYLES", "")
                append("VERSION", "1.3.0")
                append("FORMAT", "image/png")
                append("FORMAT_OPTIONS", "dpi:180")
                append("TRANSPARENT", "TRUE")
                append("TILED", "true")
                append("WIDTH", "512")
                append("HEIGHT", "512")
                append("CRS", "EPSG:3857")

                append("LAYERS", layer.joinToString(","))
                with(calculateBoundingBox(pos)) {
                    append("BBOX", "$startX,$startY,$endX,$endY")
                }
            }
        }.build()
    }
)

data class BBox(val startX: Int, val startY: Int, val endX: Int, val endY: Int)

fun calculateBoundingBox(pos: TilePos): BBox {
    val start = pos.toGeoPoint() // Left top corner of the tile
    val end = pos.copy(x = pos.x + 1, y = pos.y + 1).toGeoPoint() // Right bottom corner of the tile
    val meterStart = start.toMeter()
    val meterEnd = end.toMeter()
    val startx = min(meterEnd.x, meterStart.x)
    val starty = min(meterEnd.y, meterStart.y)
    val endx = max(meterStart.x, meterEnd.x)
    val endy = max(meterStart.y, meterEnd.y)
    return BBox(
        startX = startx.toInt(),
        startY = starty.toInt(),
        endX = endx.toInt(),
        endY = endy.toInt()
    )
}