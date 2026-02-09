package de.drick.flightlog.ui

import kotlinx.datetime.LocalDate

private fun formatDateLocalized(year: Int, month: Int, day: Int): JsString =
    js("new Date(year, month, day).toLocaleDateString()")

actual fun LocalDate.formatLocalized(): String {
    return formatDateLocalized(year, monthNumber - 1, dayOfMonth).toString()
}
