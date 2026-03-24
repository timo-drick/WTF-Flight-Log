@file:Suppress("UnusedParameter")

package de.drick.filehandling

import de.drick.core.log
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.files.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun <T : JsAny> Promise<T>.await(): T = suspendCancellableCoroutine { cont ->
    then(
        onFulfilled = { it.also { cont.resume(it) }; null },
        onRejected = { it.also { cont.resumeWithException(Throwable("Promise rejected")) }; null }
    )
}

@OptIn(ExperimentalWasmJsInterop::class)
external interface FileSystemHandle : JsAny {
    val name: String
    val kind: String
}

@OptIn(ExperimentalWasmJsInterop::class)
external interface FileSystemFileHandle : FileSystemHandle {
    fun getFile(): Promise<File>
}

@OptIn(ExperimentalWasmJsInterop::class)
external interface FileSystemDirectoryHandle : FileSystemHandle {
    fun values(): JsAny /* AsyncIterable<FileSystemHandle> */
    fun getFileHandle(name: String, options: JsAny? = definedExternally): Promise<FileSystemFileHandle>
    fun getDirectoryHandle(name: String, options: JsAny? = definedExternally): Promise<FileSystemDirectoryHandle>
}

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun asyncIterableToList(iterator: JsAny): List<JsAny> =
    suspendCancellableCoroutine { cont ->
        val result = mutableListOf<JsAny>()
        val jsIterator = getAsyncIterator(iterator)

        fun iterate() {
            val nextPromise = jsIteratorNext(jsIterator)
            nextPromise.then({ next ->
                val done = isDone(next)
                if (done) {
                    cont.resume(result)
                } else {
                    result.add(getValue(next))
                    iterate()
                }
                null
            }, {
                cont.resumeWithException(Throwable("Iteration failed"))
                null
            })
        }
        iterate()
    }

@OptIn(ExperimentalWasmJsInterop::class)
private fun getAsyncIterator(obj: JsAny): JsAny = js("obj[Symbol.asyncIterator]()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsIteratorNext(iterator: JsAny): Promise<JsAny> = js("iterator.next()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun isDone(next: JsAny): Boolean = js("next.done")

@OptIn(ExperimentalWasmJsInterop::class)
private fun getValue(next: JsAny): JsAny = js("next.value")

@OptIn(ExperimentalWasmJsInterop::class)
private fun showDirectoryPicker(): Promise<FileSystemDirectoryHandle> = js("window.showDirectoryPicker()")

@OptIn(ExperimentalWasmJsInterop::class)
suspend fun readDirectoryRecursively(dirHandle: FileSystemDirectoryHandle): List<PlatformFile> {
    println("read dir: ${dirHandle.name}")
    val files = mutableListOf<PlatformFile>()
    val entries = asyncIterableToList(dirHandle.values())
    for (entry in entries) {
        val handle = entry.unsafeCast<FileSystemHandle>()
        when (handle.kind) {
            "file" -> {
                val fileHandle = handle.unsafeCast<FileSystemFileHandle>()
                val file = fileHandle.getFile().await()
                files.add(PlatformFile(file))
            }
            "directory" -> {
                val subDir = handle.unsafeCast<FileSystemDirectoryHandle>()
                files.addAll(readDirectoryRecursively(subDir))
            }
        }
    }
    return files
}

@OptIn(ExperimentalWasmJsInterop::class)
fun isShowDirectoryPickerSupported(): Boolean = js("('showDirectoryPicker' in window)")

@OptIn(ExperimentalWasmJsInterop::class)
class DirectoryPickerChrome {

    suspend fun pickDirectory(): FileSystemDirectoryHandle? {
        if (!isShowDirectoryPickerSupported()) {
            return null
        }
        return try {
            showDirectoryPicker().await()
        } catch (e: Throwable) {
            log(e)
            null
        }
    }
}
