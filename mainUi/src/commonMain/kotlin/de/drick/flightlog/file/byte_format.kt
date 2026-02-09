package de.drick.flightlog.file

import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong

inline val BytesPerKB: Long get() = 1024L
inline val BytesPerMB: Long get() = 1024L * BytesPerKB
inline val BytesPerGB: Long get() = 1024L * BytesPerMB
inline val BytesPerTB: Long get() = 1024L * BytesPerGB
inline val BytesPerPB: Long get() = 1024L * BytesPerTB

inline val Int.bytes: ByteSize
    get() = ByteSize(this)
inline val Long.bytes: ByteSize
    get() = ByteSize(this)

inline val Number.kilobytes: ByteSize
    get() = ByteSize(BytesPerKB) * this

inline val Number.megabytes: ByteSize
    get() = ByteSize(BytesPerMB) * this

inline val Number.gigabytes: ByteSize
    get() = ByteSize(BytesPerGB) * this


@JvmInline
value class ByteSize(internal val bytes: Long) : Comparable<ByteSize> {
    constructor(bytes: Int) : this(bytes.toLong())

    val inWholeBytes: Long
        get() = bytes

    operator fun plus(other: ByteSize) = ByteSize(inWholeBytes + other.inWholeBytes)
    operator fun minus(other: ByteSize) = ByteSize(inWholeBytes - other.inWholeBytes)
    operator fun times(other: ByteSize) = ByteSize(inWholeBytes * other.inWholeBytes)
    operator fun times(other: Number) = when (other) {
        is Long -> ByteSize(inWholeBytes * other)
        is Int -> ByteSize(inWholeBytes * other)
        is Float -> ByteSize((inWholeBytes * other).roundToLong())
        is Double -> ByteSize((inWholeBytes * other).roundToLong())
        else -> error("Format not supported: $other")
    }
    override fun compareTo(other: ByteSize) = inWholeBytes.compareTo(other.inWholeBytes)

    override fun toString(): String {
        val sign = if (inWholeBytes < 0) "-" else ""
        val absBytes = abs(inWholeBytes)
        return when {
            absBytes < BytesPerKB -> "$sign${absBytes.toStringAsFixed()} byte"
            absBytes < BytesPerMB -> "$sign${(absBytes / BytesPerKB.toDouble()).toStringAsFixed()} Kb"
            absBytes < BytesPerGB -> "$sign${(absBytes / BytesPerMB.toDouble()).toStringAsFixed()} Mb"
            absBytes < BytesPerTB -> "$sign${(absBytes / BytesPerGB.toDouble()).toStringAsFixed()} Gb"
            absBytes < BytesPerPB -> "$sign${(absBytes / BytesPerTB.toDouble()).toStringAsFixed()} Tb"
            else -> "$sign${(absBytes / BytesPerPB.toDouble()).toStringAsFixed()} Pb"
        }
    }
}

private fun Long.toStringAsFixed(): String {
    return this.toDouble().toStringAsFixed()
}

private fun Double.toStringAsFixed(): String {
    return this.toStringAsFixed(digits = 2).removeSuffix(".0")
}

private fun Double.toStringAsFixed(digits: Int): String {
    val pow = 10f.pow(digits)
    val shifted = this * pow // shift the given value by the corresponding power of 10
    val decimal = shifted - shifted.toLong() // obtain the decimal of the shifted value
    // Manually round up if the decimal value is greater than or equal to 0.5f.
    // because kotlin.math.round(0.5f) rounds down
    val roundedShifted = if (decimal >= 0.5f) {
        shifted.toLong() + 1
    } else {
        shifted.toLong()
    }

    val rounded = roundedShifted / pow // divide off the corresponding power of 10 to shift back
    return if (digits > 0) {
        // If we have any decimal points, convert the float to a string
        rounded.toString()
    } else {
        // If we do not have any decimal points, return the long
        // based string representation
        rounded.toLong().toString()
    }
}

private fun Number.hasFractionalPart(): Boolean {
    return when (this) {
        is Double -> this != floor(this)
        is Float -> this != floor(this)
        else -> false
    }
}

private const val BytePrecisionLossErrorMessage =
    "ByteSize provides precision at the byte level. Representing a fractional value as " +
    "bytes may lead to precision loss. It is recommended to convert the value to a whole " +
    "number before using (Binary/Decimal)ByteSize."
