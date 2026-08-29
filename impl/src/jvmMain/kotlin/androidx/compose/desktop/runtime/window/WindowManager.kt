package androidx.compose.desktop.runtime.window

import androidx.compose.desktop.runtime.activity.ActivityManager
import androidx.compose.runtime.*
import androidx.compose.ui.window.*
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.jvm.system.di.inject
import kotlinx.coroutines.*
import org.koin.core.qualifier.named
import kotlin.getValue

/**
 * 有两种实现方式： 一种是每个window都在新的application块中调用，这个实现会比较简单。
 * 另一种实现是在这里调用application，所有window都在同一个application块中调用。
 */
class WindowManager constructor() :
    InstanceKoinComponent {
    val scope by inject<CoroutineScope>(named<ActivityManager>())

    /**
     * application所有的Window
     */
    private val windows = RootViewMgr<ApplicationScope>()

    /**
     * 无Window宿主的弹窗
     */
    private val dialogsMgr = RootViewMgr<Unit>()

    /**
     * 多窗口核心原理如下
     * ```
     * application {
     *     windows.invoke(this)
     * }
     * ```
     * 这就造成一个问题:
     * 用户想在ApplicationScope中调用Tray显示托盘菜单或是在所有compose函数外层添加内容是做不到的
     * 理想情况应该是:
     * ```
     * application {
     *    //在所有可重组内容外层添加自定义的内容
     *    MyTheme {
     *        windows.invoke(this)
     *    }
     *
     *    Tray() //托盘菜单
     * }
     * ```
     *
     * 因此, 提供了此参数,用于让用户可以获取ApplicationScope,以及操控所有compose内容显示时机
     * ```
     * startApplication<SplashActivity, MainApplication>(
     *     applicationContent = object : ApplicationRootContent {
     *         @Composable
     *         override fun ApplicationScope.invoke(content: ComposableContent) {
     *             UncaughtExceptionContent {
     *                 content() // 所有的窗口,可重组内容 关联的是 WindowManager中的AllWindowUi函数
     *                 SystemTray()
     *             }
     *         }
     *     }
     * )
     *
     * ```
     */
    var applicationRootContent: ApplicationRootContent? = null
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
            applicationRootContent?.ShowUI(scope = this, content = { AllWindowUi() }) ?: this.AllWindowUi()
        }
    }

    @Composable
    private fun ApplicationScope.AllWindowUi() {
        windows.invoke(this)
        dialogsMgr.invoke(Unit)
    }

    /**
     * 移除window，这会使window进入onDispose
     */
    fun deAttachWindow(window: ActivityRootViewEntity) {
        windows.deAttach(window)
    }

    /**
     * 添加一个要显示的window，如果添加之前没有window，则调用prepare方法。
     */
    @Synchronized
    fun attachWindow(window: ActivityRootViewEntity) {
        windows.attach(window)
    }

    /**
     * 移除Dialog
     */
    fun deAttachDialog(window: RootViewEntity<Unit>) {
        dialogsMgr.deAttach(window)
    }

    /**
     * 添加一个要显示的Dialog
     */
    @Synchronized
    fun attachDialog(window: RootViewEntity<Unit>) {
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

typealias ApplicationComposableContent = @Composable ApplicationScope.() -> Unit
typealias FrameWindowComposableContent = @Composable FrameWindowScope.() -> Unit
typealias DialogWindowComposableContent = @Composable DialogWindowScope.() -> Unit
typealias ComposableContent = @Composable () -> Unit

/**
 * 用于让用户可以手动操控applicationScope，控制所有window的显示实际,
 *
 * ```
 * object : ApplicationContentWrapper { windows ->
 *     // this: applicationScope
 *     // windows: 所有window
 *     windows()
 * }
 * ```
 */
fun interface ApplicationRootContent {

    /**
     *
     * 实现操控所有Window显示时机, 在ApplicationScope中自定义显示内容
     *
     * 实现此方法时,必须调用 content参数这个函数，否则将无法显示任何Window。
     *
     * @param content WindowManager提供的承载所有窗口的可重组函数
     * @receiver scope applicationScope
     */
    @Composable
    operator fun ApplicationScope.invoke(content: ComposableContent)
}

@Composable
internal fun ApplicationRootContent.ShowUI(scope: ApplicationScope, content: ComposableContent) =
    scope.invoke(content)

