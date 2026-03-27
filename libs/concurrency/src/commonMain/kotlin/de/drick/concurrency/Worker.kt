package de.drick.concurrency
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.KSerializer

/**
 * A worker that can execute tasks in the background.
 * On web platforms (wasmJs), it uses a WebWorker
 *
 * Example usage:
 * ```
 * // Main thread
 * val worker = BackgroundWorker("worker.js")
 * val result = worker.execute(input, Input.serializer(), Output.serializer()) { input ->
 *     // Fallback for non-web or if worker.js is missing
 *     heavyComputation(input)
 * }
 *
 * // Worker side (separate module compiled to worker.js)
 * fun main() {
 *     // Use runBackgroundWorker in wasmJsMain
 *     // runBackgroundWorker(Input.serializer(), Output.serializer()) { input ->
 *     //     heavyComputation(input)
 *     // }
 * }
 * ```
 */
interface BackgroundWorker<T> {
    suspend fun execute(
        input: PlatformFile,
    ): T

    fun terminate()
}

expect suspend fun <T>createBackgroundWorkerInstance(
    workerScriptUrl: String,
    nonWebImplementation: (PlatformFile) -> T,
    outputSerializer: KSerializer<T>,
): BackgroundWorker<T>
