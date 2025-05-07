package androidx.compose.desktop.runtime.window

import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.application
import java.util.*

/**
 * @param activity
 * @param multiApplication true：
 *    window和application一一对应；false：在一个application中创建多个window。
 */
class DxWindowHolder(
    val activity: Activity,
    private val windowManager: WindowManager,
    private val multiApplication: Boolean = false,
    val uuid: UUID = UUID.randomUUID(),
) {
    lateinit var composeWindow: ComposeWindow
        internal set

    /**
     * 是否被添加到了[WindowManager.windows]列表中
     */
    @Volatile
    var isAttachedToApplication: Boolean = false

    //setContentView传入的内容，需要显示的页面内容
    var rootView: (@Composable ApplicationScope.() -> Unit)? = null

    @Composable
    fun windowExec(scope: ApplicationScope) {
        rootView?.invoke(scope)
    }


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
    infix fun show(content: @Composable() (ApplicationScope.() -> Unit)) {
        this.rootView = content
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

    fun active() {
        exit.value = false
        this.rootView?.let { this@DxWindowHolder show it }
    }

    fun release() {
        exit.value = true
        windowManager.deAttachWindow(this)
    }

    fun isAttached(): Boolean {
        return isAttachedToApplication && this::composeWindow.isInitialized
    }

}

enum class WindowSizeState {
    Max, Min, Restore
}

