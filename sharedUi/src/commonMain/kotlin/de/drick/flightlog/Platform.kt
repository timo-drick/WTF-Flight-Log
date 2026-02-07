package de.drick.flightlog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform