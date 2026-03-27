package de.drick.flightlog.file

import de.drick.wtf_osd.FontVariant
import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.LonLat
import de.drick.wtf_osd.Height
import de.drick.wtf_osd.Speed
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration

data class LogItem(
    val name: String,
    val files: ImmutableList<FileItem>
) {
    val lastModified: Instant? = files.mapNotNull { it.lastModified }.maxOrNull()
}

@Serializable
sealed interface FileItem {
    val name: String
    val extension: String
    val path: String
    val size: ByteSize
    val lastModified: Instant?
    suspend fun source(): Source
    fun platformFile(): PlatformFile
}

@Serializable
data class TestFileItem(
    override val name: String,
    override val extension: String,
    override val path: String,
    @Serializable(with = ByteSizeSerializer::class)
    override val size: ByteSize,
    override val lastModified: Instant?
): FileItem {
    override suspend fun source(): Source {
        TODO("Not implemented for test")
    }
    override fun platformFile(): PlatformFile {
        TODO("Not implemented for test")
    }
}

fun FileItem.fromPlatformFile(file: PlatformFile) = BaseFile(file)

suspend fun PlatformFile.toSource() = Buffer().apply {
    write(readBytes())
}

@Serializable
data class BaseFile(
    val file: PlatformFile
) : FileItem {
    override val name = file.nameWithoutExtension
    override val extension = file.extension
    @Serializable(with = ByteSizeSerializer::class)
    override val size = file.size().bytes
    override val lastModified = file.lastModifiedTime()
    override val path = file.path().substringBeforeLast('/')
    override suspend fun source() = file.toSource()
    override fun platformFile() = file
}

data class ErrorFile(
    val file: FileItem,
    val message: String
) : FileItem by file

data class VideoFile(
    val file: FileItem,
    //TODO maybe add duration
) : FileItem by file

@Serializable
data class OSDFile(
    val file: FileItem,
    val fontVariant: FontVariant,
    val duration: Duration,
    val hasGpsData: Boolean,
    val startPosition: GeoPoint?,
    val aircraftIdentifier: String?,
    val maxSpeed: Speed?,
    val maxHeight: Height?,
    val distanceTotal: Double?,
    val maxDistanceHome: Double?
) : FileItem by file

@Serializable
@SerialName("SrtSummery")
data class SrtSummeryData(
    val duration: Duration
)

@Serializable
data class SRTFile(
    val file: FileItem,
    val duration: Duration
) : FileItem by file

@Serializable
data class FontFile(
    val file: FileItem,
    val fontVariant: FontVariant
) : FileItem by file


object ByteSizeSerializer : KSerializer<ByteSize> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ByteSize", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: ByteSize) =
        encoder.encodeLong(value.bytes)
    override fun deserialize(decoder: Decoder): ByteSize {
        return decoder.decodeLong().bytes
    }
}
