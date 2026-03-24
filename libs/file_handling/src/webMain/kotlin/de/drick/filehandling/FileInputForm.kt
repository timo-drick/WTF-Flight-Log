package de.drick.filehandling

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.files.FileList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalWasmJsInterop::class)
internal suspend fun openFilePicker(
    type: FileKitType,
    multipleMode: Boolean,
    directoryMode: Boolean,
) = withContext(Dispatchers.Default) {
    suspendCancellableCoroutine { continuation ->
        // Create input element
        val input = document.createElement("input") as HTMLInputElement

        // Visually hide the element
        input.style.display = "none"

        document.body?.appendChild(input)
        // Configure the input element
        input.apply {
            this.type = "file"
            // Set the allowed file types
            when (type) {
                is FileKitType.Image -> accept = "image/*"
                is FileKitType.Video -> accept = "video/*"
                is FileKitType.ImageAndVideo -> accept = "image/*,video/*"
                is FileKitType.File -> type.extensions?.let { ext ->
                    accept = ext.joinToString(",") { ".$it" }
                }
            }

            // Set the multiple attribute
            multiple = multipleMode
            webkitdirectory = directoryMode

            // max is not supported for file inputs
        }

        // Setup the change listener
        input.onchange = { event ->
            try {
                // Get the selected files
                val files = event.target
                    ?.unsafeCast<HTMLInputElement>()
                    ?.files
                    ?.asList()

                // Return the result
                val result = files?.map { PlatformFile(it) }
                continuation.resume(result)
            } catch (e: Throwable) {
                continuation.resumeWithException(e)
            } finally {
                document.body?.removeChild(input)
            }
        }

        input.oncancel = {
            continuation.resume(null)
            document.body?.removeChild(input)
        }

        // Trigger the file picker
        input.click()
    }
}


@OptIn(ExperimentalWasmJsInterop::class)
private abstract external class HTMLInputElement : HTMLElement, JsAny {
    open var accept: String
    open val files: FileList?
    open var multiple: Boolean
    open var webkitdirectory: Boolean
    open var type: String
    open var value: String
}
