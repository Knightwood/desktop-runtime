package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 记录applicationScope状态
 */
class ApplicationScopeToken(
    /**
     * 启用多Application特性启动Activity, 会使用独立的applicationScope显示根视图,
     * 而不会将根视图放入WindowManager使用全局applicationScope显示.
     * 你依旧可以使用finish结束此Activity,会连同独立的application一并销毁.
     *
     * 此变量记录启动独立application的协程job
     */
    var thread: Thread? = null,
) {
    val isExist get() = thread != null
    val isAlive get() = thread?.isAlive ?: false

    /**
     * 记录是否销毁ApplicationScope,不再显示Window
     */
    var destroy by mutableStateOf(false)
        private set

    /**
     * 销毁ApplicationScope,不再显示Window
     */
    fun dismiss() {
        destroy = true
        // 当destroy为true时由于重组, applicationScope不再显示任何窗口并自动终止, thread也因此自动释放, 调用此方法无实际作用.
        thread?.interrupt()
    }
}
