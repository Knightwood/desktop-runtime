package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable

fun interface ContentWrapper {
    fun wrap(content: @Composable () -> Unit): @Composable () -> Unit
}
