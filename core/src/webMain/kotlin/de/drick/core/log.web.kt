package de.drick.core

actual fun logPlatform(error: Throwable?, msg: () -> String) {
    println(msg())
    error?.printStackTrace()
}