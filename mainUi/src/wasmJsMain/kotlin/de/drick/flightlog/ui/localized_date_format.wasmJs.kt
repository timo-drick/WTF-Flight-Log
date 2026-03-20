package de.drick.flightlog.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

@OptIn(ExperimentalWasmJsInterop::class)
private fun formatDateLocalized(year: Int, month: Int, day: Int): JsString =
    js("new Date(year, month, day).toLocaleDateString()")

@OptIn(ExperimentalWasmJsInterop::class)
actual fun LocalDate.formatLocalized(): String {
    return formatDateLocalized(year, month.number - 1, day).toString()
}
