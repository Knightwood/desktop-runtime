package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.application
import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.ui.window.ApplicationScope

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
    /**
     * 这只是一个标记，实际只有在deAttach为真时起作用。
     *
     */
    private val exit: MutableState<Boolean> = mutableStateOf(false)

    /**
     * 这只是一个标记，在deAttach为真时可以视为是否退出。
     */
    val isReleased: Boolean
        get() = exit.value

    /**
     * 1. 如果设置为多application，则直接使用新的application运行ui界面
     * 2. 如果设置为单application，则将自己注册进[WindowManager]
     */
    operator fun invoke(content: @Composable() (ApplicationScope.() -> Unit)) {
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
