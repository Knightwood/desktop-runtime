package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skiko.MainUIDispatcher

typealias ApplicationContent = @Composable ApplicationScope.() -> Unit

/**
 * 用于让用户可以手动操控applicationScope，以及内容显示 用法：实现接口，并手动调用接口方法中的函数参数
 *
 * ```
 * object:ApplicationContentWrapper { scope, windows ->
 *         // scope：applicationScope，windows：所有window列表
 *         scope.windows()
 * }
 * ```
 */
fun interface ApplicationContentWrapper {
    /**
     * 显示应用程序内容
     */
    @Composable
    fun show(scope: ApplicationScope, content: ApplicationContent)
}

/**
 * 有两种实现方式： 一种是每个window都在新的application块中调用，这个实现会比较简单。
 * 另一种实现是在这里调用application，所有window都在同一个application块中调用。
 */
class WindowManager private constructor() {
    val scope = CoroutineScope(MainUIDispatcher) + SupervisorJob() + CoroutineName("ActivityManager")

    private val windows: SnapshotStateList<DxWindowHolder> = SnapshotStateList()

    //    private val exit: MutableState<Boolean> = mutableStateOf(false)
    var contentWrapper = ApplicationContentWrapper { scope, windows ->
        scope.windows()
    }
    private var b = MutableStateFlow<Boolean>(false)

    /**
     * 调用application方法，监听windows列表变化，并创建窗口内容。
     * 我希望这里观察[windows]的变化，并调用[DxWindowHolder]的[DxWindowHolder.windowExec]方法以展示内容。
     * 但同时希望尽可能减少重组，提升性能。
     */
    fun prepare() {
        //调用此函数，主线程就陷入阻塞了，所以需要注意。
        //exitProcessOnExit = false 避免主线程结束
        application(exitProcessOnExit = false) {
            contentWrapper.show(this) { RunUI() }
            val state = b.collectAsState()
            if (state.value) {
                Unit
            }
        }
    }

    @Composable
    fun ApplicationScope.RunUI() {
        windows.forEach { current ->
            key(current) {//避免无谓的重组
                current.windowExec(this)
            }
        }
    }

    /**
     * 移除window，这会使window进入onDispose
     */
    fun deAttachWindow(window: DxWindowHolder) {
        window.isAttachedToApplication = false
        windows.remove(window)
    }

    /**
     * 添加一个要显示的window，如果添加之前没有window，则调用prepare方法。
     */
    @Synchronized
    fun attachWindow(window: DxWindowHolder) {
        if (window.isAttachedToApplication) return
        windows.add(window)
        window.isAttachedToApplication = true
    }

    fun release() {
        windows.forEach {
            it.release()
        }
        windows.clear()
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
