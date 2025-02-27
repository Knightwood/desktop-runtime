package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.application
import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.ui.window.ApplicationScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @param activity
 * @param multiApplication true：
 *    window和application一一对应；false：在一个application中创建多个window。
 */
class DesktopWindow(
    val activity: Activity,
    private val windowManager: WindowManager,
    private val multiApplication: Boolean = false,
) : DxWindow() {
    /**
     * 是否隐藏Window
     */
    var isHidden = mutableStateOf<Boolean>(false)

    /**
     * 在多Application模式下，这用于是否结束Application。
     *
     * 在单Application模式下，这个值没有用，仅作为标记。
     */
    val exit: MutableState<Boolean> = mutableStateOf(false)

    /**
     * 1. 如果设置为多application，则直接使用新的application运行ui界面
     * 2. 如果设置为单application，则将自己注册进[WindowManager]
     */
    operator fun invoke(content: @Composable() (ApplicationScope.() -> Unit)) {
        this.contentShell = content
        if (multiApplication) {
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

    override fun active() {
        exit.value = false
        this.contentShell?.let { this@DesktopWindow.invoke(it) }
    }

    override fun release() {
        exit.value = true
        windowManager.deAttachWindow(this)
    }
}
