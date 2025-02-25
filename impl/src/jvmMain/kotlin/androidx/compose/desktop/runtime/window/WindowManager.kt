package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import org.jetbrains.skiko.MainUIDispatcher

/**
 * 有两种实现方式： 一种是每个window都在新的application块中调用，这个实现会比较简单。
 * 另一种实现是在这里调用application，所有window都在同一个application块中调用。
 */
class WindowManager private constructor() {
    val scope = CoroutineScope(MainUIDispatcher) + SupervisorJob() + CoroutineName("ActivityManager")

    private val windows: SnapshotStateList<DxWindow> = SnapshotStateList()
    private val exit: MutableState<Boolean> = mutableStateOf(false)
    var content: (@Composable ApplicationScope.() -> Unit)? = null

    /**
     * 调用application方法，监听windows列表变化，并创建窗口内容。
     * 我希望这里观察[windows]的变化，并调用[DxWindow]的[DxWindow.windowExec]方法以展示内容。
     * 但同时希望尽可能减少重组，提升性能。
     */
    fun prepare() {
        //调用此函数，主线程就陷入阻塞了，所以需要注意。
        //exitProcessOnExit = false 避免主线程结束
        application(exitProcessOnExit = false) {
            this@WindowManager.content?.invoke(this)
            windows.forEach { current ->
                current.windowExec(this)
            }
        }
    }

    fun deAttachWindow(window: DxWindow) {
        windows.remove(window)
    }

    /**
     * 添加一个要显示的window，如果添加之前没有window，则调用prepare方法。
     */
    fun attachWindow(window: DxWindow) {
        windows.add(window)
    }

    companion object {
        const val NAME = "WindowManager"

        @Volatile
        private var singleInstance: WindowManager? = null

        fun instance(): WindowManager {
            return singleInstance ?: synchronized(this) {
                singleInstance ?: WindowManager().also { singleInstance = it }
            }
        }
    }

}
