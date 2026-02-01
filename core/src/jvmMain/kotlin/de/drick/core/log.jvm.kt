package de.drick.core

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual fun logPlatform(error: Throwable?, msg: () -> String) {
    logJvm(msg(), error)
}

const val logFileName = "log.kt"

fun logJvm(msg: Any, error: Throwable? = null) {
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
    val dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    println("$dateTime $threadName: $message")
    error?.printStackTrace()
}