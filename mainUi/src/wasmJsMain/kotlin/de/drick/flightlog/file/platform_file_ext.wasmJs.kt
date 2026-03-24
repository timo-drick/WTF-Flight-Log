package de.drick.flightlog.file

import io.github.vinceglb.filekit.PlatformFile
import org.w3c.files.Blob
import org.w3c.files.FilePropertyBag
import kotlin.time.Instant

@OptIn(ExperimentalWasmJsInterop::class)
actual fun PlatformFile.lastModifiedTime(): Instant? {
    val ts = file.unsafeCast<File>().lastModified
    return Instant.fromEpochMilliseconds(ts.toDouble().toLong())
}

@OptIn(ExperimentalWasmJsInterop::class)
actual fun PlatformFile.path() = file.unsafeCast<File>().webkitRelativePath ?: ""

@OptIn(ExperimentalWasmJsInterop::class)
open external class File(
    fileBits: JsArray<JsAny? /* BufferSource|Blob|String */>,
    fileName: String, options: FilePropertyBag = definedExternally
) : Blob, JsAny {
    val name: String
    val lastModified: JsNumber
    val webkitRelativePath: String?
}
