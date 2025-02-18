package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.application
import androidx.compose.desktop.runtime.activity.Activity

/**
 * @param activity
 * @param deAttach true：
 *    window和application一一对应；false：在一个application中创建多个window。
 */
class DesktopWindow(
    val activity: Activity,
    private val windowManager: WindowManager,
    private val deAttach: Boolean = false,
) : DxWindow() {
    private val exit: MutableState<Boolean> = mutableStateOf(false)

    val isReleased: Boolean
        get() = exit.value

    operator fun invoke(content: @Composable() (() -> Unit)) {
        this.content = content
        if (deAttach) {
            application {
                if (exit.value) {
                    exitApplication()
                }
                content()
            }
        } else {
            windowManager.attachWindow(this)
        }
    }

    override fun release() {
        super.release()
        exit.value = true
    }
}
