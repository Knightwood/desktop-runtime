package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.window.*
import androidx.jvm.system.di.InstanceKoinComponent
import kotlinx.coroutines.*
import org.jetbrains.skiko.MainUIDispatcher

typealias ApplicationComposableContent = @Composable ApplicationScope.() -> Unit
typealias WindowComposableContent = @Composable FrameWindowScope.() -> Unit
typealias DialogWindowComposableContent = @Composable DialogWindowScope.() -> Unit
typealias ComposableContent = @Composable () -> Unit

/**
 * 用于让用户可以手动操控applicationScope，以及内容显示 用法：实现接口，并手动调用接口方法中的函数参数
 *
 * ```
 * object:ApplicationContentWrapper { windows ->
 *         // scope：applicationScope，windows：所有window列表
 *         windows()
 * }
 * ```
 */
fun interface ApplicationContentWrapper {

    /**
     * 显示应用程序内容
     *
     * @param content 所有的窗口
     *
     * 在此方法的实现中必须手动调用 content()，否则将无任何界面显示。
     *
     * @receiver scope applicationScope
     */
    @Composable
    operator fun ApplicationScope.invoke(content: ComposableContent)
}

@Composable
internal fun ApplicationContentWrapper.ShowUI(scope: ApplicationScope, content: ComposableContent) =
    scope.invoke(content)

/**
 * 有两种实现方式： 一种是每个window都在新的application块中调用，这个实现会比较简单。
 * 另一种实现是在这里调用application，所有window都在同一个application块中调用。
 */
class WindowManager constructor() :
    InstanceKoinComponent {
    val scope = CoroutineScope(MainUIDispatcher) + SupervisorJob() + CoroutineName("ActivityManager")

    private val windows: SnapshotStateList<ActivityRootViewEntity> = SnapshotStateList()

    /**
     * 存储无宿主Window的弹窗
     */
    private val dialogsMgr = RootViewMgr<FrameWindowScope?, DialogRootViewEntity>()
    var contentWrapper: ApplicationContentWrapper? = null
        internal set

    private var applicationScope: ApplicationScope? = null

    /**
     * 调用application方法，监听windows列表变化，并创建窗口内容。
     * 我希望这里观察[windows]的变化，并调用[ActivityRootViewEntity]的[ActivityRootViewEntity.windowExec]方法以展示内容。
     * 但同时希望尽可能减少重组，提升性能。
     */
    fun prepare() {
        //调用此函数，主线程就陷入阻塞了，所以需要注意。
        //exitProcessOnExit = false 避免主线程结束
        application(exitProcessOnExit = false) {
            this@WindowManager.applicationScope = this
            contentWrapper?.ShowUI(scope = this, content = { AllWindowUi() }) ?: this.AllWindowUi()
        }
    }

    @Composable
    private fun ApplicationScope.AllWindowUi() {
        windows.forEach { current ->
            key(current) {//避免无谓的重组
                current.rootContent?.invoke(this)
            }
        }
        dialogsMgr.Content(null)
    }

    /**
     * 移除window，这会使window进入onDispose
     */
    fun deAttachWindow(window: ActivityRootViewEntity) {
        window.isAttached = false
        windows.remove(window)
    }

    /**
     * 添加一个要显示的window，如果添加之前没有window，则调用prepare方法。
     */
    @Synchronized
    fun attachWindow(window: ActivityRootViewEntity) {
        if (window.isAttached) return
        windows.add(window)
        window.isAttached = true
    }

    /**
     * 移除Dialog
     */
    fun deAttachDialog(window: DialogRootViewEntity) {
       dialogsMgr.deAttach(window)
    }

    /**
     * 添加一个要显示的Dialog。
     */
    @Synchronized
    fun attachDialog(window: DialogRootViewEntity) {
       dialogsMgr.attach(window)
    }

    fun release() {
        windows.clear()
        dialogsMgr.clear()
    }

    fun exitApplication() {
        applicationScope?.exitApplication()
    }

    fun isEmpty(): Boolean {
        return windows.isEmpty()
    }

}
