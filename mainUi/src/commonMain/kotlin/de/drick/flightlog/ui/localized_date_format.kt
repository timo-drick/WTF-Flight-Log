package de.drick.flightlog.ui

import kotlinx.datetime.LocalDate

/**
 * Formats a [LocalDate] as a localized date string using the system's default locale.
 * Works on all KMP targets including wasmJs.
 */
expect fun LocalDate.formatLocalized(): String
