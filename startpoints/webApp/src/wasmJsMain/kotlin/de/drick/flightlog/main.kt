package de.drick.flightlog

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = checkNotNull(document.body)
    ComposeViewport(body) {
        App()
    }
}