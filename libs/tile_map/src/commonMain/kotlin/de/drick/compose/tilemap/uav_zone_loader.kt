package de.drick.compose.tilemap

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient


/**
 * Data from https://dnt.anac.pt/mapa.html
 */
private const val UAV_ZONE_URL = "https://dnt.anac.pt/mapa_UASZoneVersion.js"

@Serializable
data class UASZoneData(
    val title: String,
    val description: String,
    val features: List<UASZone>
)

@Serializable
data class UASZone(
    val identifier: String,
    val country: String,
    val name: String,
    val type: String,
    val restriction: String,
    val reason: List<String> = emptyList(),
    val otherReasonInfo: String = "",
    val applicability: List<Applicability> = emptyList(),
    val message: String = "",
    val zoneAuthority: List<ZoneAuthority> = emptyList(),
    val geometry: List<ZoneGeometry> = emptyList(),
    val extendedProperties: ExtendedProperties? = null
)

@Serializable
data class Applicability(
    val startDateTime: String,
    val endDateTime: String,
    val permanent: String = ""
)

@Serializable
data class ZoneAuthority(
    val name: String,
    val service: String = "",
    val contactName: String = "",
    val siteURL: String = "",
    val email: String = "",
    val phone: String = "",
    val purpose: String = ""
)

@Serializable
data class ZoneGeometry(
    val upperLimit: Double = 0.0,
    val lowerLimit: Double = 0.0,
    val uomDimensions: String = "",
    val upperVerticalReference: String = "",
    val lowerVerticalReference: String = "",
    val horizontalProjection: HorizontalProjection
)

@Serializable
sealed class HorizontalProjection {
    abstract val type: String
}

object LonLatSerializer : KSerializer<LonLat> {
    private val delegate = ListSerializer(Double.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun serialize(encoder: Encoder, value: LonLat) =
        delegate.serialize(encoder, listOf(value.lon, value.lat))
    override fun deserialize(decoder: Decoder): LonLat {
        val list = delegate.deserialize(decoder)
        return LonLat(lon = list[0], lat = list[1])
    }
}

@Serializable(with = LonLatSerializer::class)
data class LonLat(val lon: Double, val lat: Double)

@Serializable
@SerialName("Circle")
data class CircleProjection(
    override val type: String,
    val center: LonLat,
    val radius: Double
) : HorizontalProjection()

@Serializable
@SerialName("Polygon")
data class PolygonProjection(
    override val type: String,
    val coordinates: List<List<LonLat>>
) : HorizontalProjection()

@Serializable
data class ExtendedProperties(
    val color: String = "",
    val arc: String = ""
)

internal val uavHttpClient by lazy { HttpClient() }

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    classDiscriminator = "type"
}

private val hexEscapeRegex = Regex("""\\x([0-9a-fA-F]{2})""")

fun decodeUavZones(data: String): UASZoneData {
    val jsonText = data
        .removePrefix("data = ")
        .replace("'", "\"")
        .replace(hexEscapeRegex) { match ->
            val codePoint = match.groupValues[1].toInt(16)
            codePoint.toChar().toString()
        }
    return json.decodeFromString<UASZoneData>(jsonText)
}

suspend fun loadUavZones(): UASZoneData? {
    val response = uavHttpClient.get(UAV_ZONE_URL)
    if (!response.status.isSuccess()) return null
    val text = response.bodyAsText()
    return decodeUavZones(text)
}
