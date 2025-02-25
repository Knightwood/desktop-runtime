package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import java.util.*

abstract class DxWindow(
    val uuid: UUID = UUID.randomUUID(),
) {
    var content: (@Composable ApplicationScope.() -> Unit)? = null

    @Composable
    fun windowExec(scope: ApplicationScope) {
        content?.invoke(scope)
    }

    open fun release() {

    }
}

