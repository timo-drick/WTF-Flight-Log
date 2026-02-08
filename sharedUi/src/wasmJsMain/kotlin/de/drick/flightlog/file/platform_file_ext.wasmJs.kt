package de.drick.flightlog.file

import io.github.vinceglb.filekit.PlatformFile
import kotlin.time.Instant

@OptIn(ExperimentalWasmJsInterop::class)
actual fun PlatformFile.lastModifiedTime(): Instant? {
    val ts = getLastModifiedDouble(file)
    return Instant.fromEpochMilliseconds(ts.toLong())
}

/**
 * Workaround of wrong defined lastModified return type in org.w3c.files.File
 */
@OptIn(ExperimentalWasmJsInterop::class)
private fun getLastModifiedDouble(file: JsAny): Double = js("file.lastModified")
