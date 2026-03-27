package de.drick.concurrency

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

actual suspend fun <T>createBackgroundWorkerInstance(
    workerScriptUrl: String,
    nonWebImplementation: (PlatformFile) -> T,
    outputSerializer: KSerializer<T>,
) = object : BackgroundWorker<T> {
    override suspend fun execute(
        input: PlatformFile,
    ): T = withContext(Dispatchers.Default) {
        nonWebImplementation(input)
    }

    override fun terminate() {
        //Noop
    }
}
