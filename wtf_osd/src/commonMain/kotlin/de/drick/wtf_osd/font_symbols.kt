package de.drick.wtf_osd


val FontVariant.LAT: Int? get() = when(this) {
    FontVariant.BETAFLIGHT -> 137
    FontVariant.INAV -> 3
    else -> null
}

val FontVariant.LON: Int? get() = when(this) {
    FontVariant.BETAFLIGHT -> 152
    FontVariant.INAV -> 4
    else -> null
}

fun ShortArray.detectTrailingString(fontVariant: FontVariant, code: Int, number: Int): String? {
    val chars = when (fontVariant) {
        FontVariant.INAV -> detectTrailingChars(code, number)?.mapINavAscii()
        else -> detectTrailingChars(code, number)
    }
    return chars?.toNullString()
}

private fun ShortArray.detectTrailingChars(code: Int, number: Int): CharArray? {
    val shortCode = code.toShort()
    val index = indexOfFirst { it == shortCode }
    return if (index >= 0) {
        CharArray(number) {
            val short = this[index + it + 1]
            Char(short.toInt())
        }
    } else {
        null
    }
}


private fun CharArray.toNullString(): String {
    val endIndex = indexOf('\u0000')
    return if (endIndex >= 0) {
        concatToString(startIndex = 0, endIndex)
    } else {
        concatToString()
    }
}

private fun CharArray.mapINavAscii() = flatMap {
    when (it.code) {
        161 -> listOf('0')     // This character includes a half decimal point
        162 -> listOf('1')     // the other half is than added with characters 177-186
        163 -> listOf('2')
        164 -> listOf('3')
        165 -> listOf('4')
        166 -> listOf('5')
        167 -> listOf('6')
        168 -> listOf('7')
        169 -> listOf('8')
        170 -> listOf('9')
        177 -> listOf('.','0') // second half of decimal point and number
        178 -> listOf('.','1')
        179 -> listOf('.','2')
        180 -> listOf('.','3')
        181 -> listOf('.','4')
        182 -> listOf('.','5')
        183 -> listOf('.','6')
        184 -> listOf('.','7')
        185 -> listOf('.','8')
        186 -> listOf('.','9')
        else -> listOf(it)
    }
}.toCharArray()