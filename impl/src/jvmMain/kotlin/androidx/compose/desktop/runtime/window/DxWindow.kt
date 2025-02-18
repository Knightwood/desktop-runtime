package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import java.util.*

abstract class DxWindow(
    val uuid: UUID = UUID.randomUUID(),
) {
    var content: (@Composable () -> Unit)? = null

    @Composable
    fun windowExec() {
        content?.invoke()
    }

    open fun release() {

    }
}

