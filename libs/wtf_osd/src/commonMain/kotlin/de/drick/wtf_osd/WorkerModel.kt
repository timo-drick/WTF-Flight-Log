package de.drick.wtf_osd

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
@SerialName("OSDSummery")
data class OSDSummeryData(
    val fontVariant: FontVariant,
    val duration: Duration,
    val hasGpsData: Boolean,
    val startPosition: GpsPoint?,
    val aircraftIdentifier: String?,
    val maxSpeed: Speed?,
    val maxHeight: Height?,
    val distanceTotal: Double?,
    val maxDistanceHome: Double?
)

@Serializable
sealed interface FileParserResult {

    @Serializable
    data class ErrorResult(
        val message: String
    ): FileParserResult

    @Serializable
    data class OsdResult(
        val data: OSDSummeryData
    ): FileParserResult

}