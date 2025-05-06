package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.ApplicationScope
import java.awt.AWTEvent
import java.util.*

abstract class DxWindow(
    val uuid: UUID = UUID.randomUUID(),
) {
    lateinit var composeWindow: ComposeWindow
        internal set
    /**
     * 是否被添加到了[WindowManager.windows]列表中
     */
    @Volatile
    var isAttachedToApplication: Boolean = false

    //setContentView传入的内容
    var contentShell: (@Composable ApplicationScope.() -> Unit)? = null

    @Composable
    fun windowExec(scope: ApplicationScope) {
        contentShell?.invoke(scope)
    }

    abstract fun release()
    abstract fun active()

}

enum class WindowSizeState {
    Max, Min, Restore
}

