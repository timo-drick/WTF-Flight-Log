package de.drick.concurrency

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.w3c.dom.ErrorEvent
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import org.w3c.files.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsString

private external val self: Worker

@OptIn(ExperimentalWasmJsInterop::class, kotlinx.coroutines.DelicateCoroutinesApi::class)
fun <T> runBackgroundWorker(
    outputSerializer: KSerializer<T>,
    block: suspend (PlatformFile) -> T
) {
    println("Start background handler")
    self.onmessage = { event ->
        println("Message received")
        val messageEvent = event.unsafeCast<MessageEvent>()
        when (val data = messageEvent.data?.unsafeCast<File>()) {
            null -> println("No data received!")
            else -> {
                val input = PlatformFile(data)
                GlobalScope.launch {
                    try {
                        val result = block(input)
                        val jsonResult = Json.encodeToString(outputSerializer, result)
                        self.postMessage(jsonResult.toJsString())
                    } catch (e: Throwable) {
                        println("Error in worker: $e")
                    }
                }
            }
        }
    }
    self.postMessage("init".toJsString())
}

actual suspend fun <T>createBackgroundWorkerInstance(
    workerScriptUrl: String,
    nonWebImplementation: (PlatformFile) -> T,
    outputSerializer: KSerializer<T>,
): BackgroundWorker<T> = BackgroundWorkerImpl(workerScriptUrl, outputSerializer).apply {
    init()
}

@OptIn(ExperimentalWasmJsInterop::class)
class BackgroundWorkerImpl<T>(
    workerScriptUrl: String,
    val outputSerializer: KSerializer<T>,
) : BackgroundWorker<T> {
    private val worker: Worker = Worker(workerScriptUrl)

    suspend fun init() {
        suspendCancellableCoroutine { cont ->
            println("Wait for init worker")
            worker.onmessage = { event: Event ->
                println("First event received")
                cont.resume(Unit)
            }
        }
    }

    override suspend fun execute(
        input: PlatformFile
    ): T = suspendCancellableCoroutine { cont ->
        val onMessage = { event: Event ->
            val messageEvent = event.unsafeCast<MessageEvent>()
            val data = messageEvent.data?.unsafeCast<JsString>()?.toString()
            try {
                if (data != null) {
                    val result = Json.decodeFromString(outputSerializer, data)
                    cont.resume(result)
                } else {
                    cont.resumeWithException(RuntimeException("Worker received null data"))
                }
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }

        val onError = { event: Event ->
            println("Error event: $event")
            val errorEvent = event.unsafeCast<ErrorEvent>()
            cont.resumeWithException(RuntimeException("Worker error: ${errorEvent.message}"))
        }
        worker.onmessage = onMessage
        worker.onerror = onError

        worker.postMessage(input.file.unsafeCast())

        cont.invokeOnCancellation {
            worker.onmessage = null
            worker.onerror = null
        }
    }

    override fun terminate() {
        worker.terminate()
    }
}
