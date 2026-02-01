package de.drick.core

import android.util.Log

private const val logFileName = "log.kt"

actual fun logPlatform(error: Throwable?, msg: () -> String) {
    val (threadName, message) = analyze(msg())
    if (error != null) {
        Log.e(threadName, message, error)
    } else {
        Log.d(threadName, message)
    }
}

private fun analyze(msg: String?): Pair<String, String> {
    val ct = Thread.currentThread()
    val threadName = ct.name
    val traces = ct.stackTrace
    val max = traces.size-1
    val stackTrace = traces.slice(3..max).find { it.fileName != logFileName }
    val message = if (stackTrace != null) {
        val cname = stackTrace.className.substringAfterLast(".")
        "[${stackTrace.fileName}:${stackTrace.lineNumber}] $cname.${stackTrace.methodName} : $msg"
    } else {
        "$msg"
    }
    return Pair(threadName, message)
}