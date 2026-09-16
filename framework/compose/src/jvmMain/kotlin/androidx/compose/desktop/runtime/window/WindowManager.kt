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
 * 窗口管理器，负责所有窗口与无宿主弹窗的挂载、显示与销毁。
 *
 * 使用[application]方法启动一个compose应用，在content函数实现中调用[Window]、[DialogWindow]显示窗口，
 * 我们暂且称content参数函数为"应用根视图".
 *
 * ## 职责
 *
 * - 维护"当前应显示的窗口"列表，并在 应用根视图 中逐一渲染
 * - 维护"无窗口宿主的弹窗"列表，与窗口一起在同一 应用根视图 中渲染
 * - 调用 `application {}` 开始应用的生命周期（见 [prepare]）
 * - 允许用户通过 [ApplicationRootContent] 自定义 应用根视图显示内容
 *
 * ## 实现原理
 *
 * 核心是"用一个可观察列表驱动重组"：
 * 将每个内部调用[Window]的 Compose 函数放入 [SnapshotStateList]，在 `application {}` 中遍历并调用，
 * 列表变化即触发重组：加入则窗口显示，移除则窗口进入 `onDispose` 并不再显示。
 *
 * ```
 * typealias ComposeContent = @Composable () -> Unit
 * val windows = SnapshotStateList<ComposeContent>()
 * application {
 *     windows.forEach { it() }
 * }
 *
 * val window1 : ComposeContent = {
 *     Window(onCloseRequest = {})
 * }
 *
 * // 显示窗口
 * windows.add(window1)
 * // 关闭窗口
 * windows.remove(window1)
 * ```
 *
 * ## 线程模型
 *
 * - [prepare] 会阻塞调用线程（内部进入 Compose 主循环），应仅在主线程调用一次
 * - [deAttachWindow]、[deAttachDialog]、[release]、[exitApplication]、[isEmpty]应在主线程调用
 *
 * @see application
 */
class WindowManager constructor() :
    InstanceKoinComponent {

    /** 与 [ActivityManager] 绑定的协程作用域，提供Swing UI 线程 */
    val scope by inject<CoroutineScope>(named<ActivityManager>())

    private var isPrepared = false

    /** Activity根视图集合，存储所有要显示的窗口集合 */
    private val windows = RootViewMgr<ApplicationScope>()

    /** 无 Window 宿主的弹窗根视图集合 */
    private val dialogs = RootViewMgr<Unit>()

    /**
     * 用于用户自定义 应用根视图，默认为 null。
     *
     * 为 null 时使用默认根视图（直接渲染 [AllWindows]）；
     * 非 null 时由用户决定Activity根视图显示时机、提供自定义窗口内容、托盘，参见 [ApplicationRootContent]。
     */
    var userInsteadApplicationRootContent: ApplicationRootContent? = null
        internal set

    /** 当前 Compose `application {}` 的作用域，[prepare] 调用后赋值。 */
    private var applicationScope: ApplicationScope? = null

    /**
     * 启动 Compose `application {}` 主循环，监听 [windows] 与 [dialogs] 的变化并渲染内容。
     *
     * 框架内调用，不可直接调用
     *
     * 用户可通过 [userInsteadApplicationRootContent] 自定义 Application 的根视图，
     * 实现：
     * 1. 提供系统托盘、提供脱离此框架的窗口显示
     * 2. 将框架内要显示的窗口显示时机委托给用户，实现为所有窗口提供主题、异常处理、提供[CompositionLocal]值等
     *
     * **注意**：调用此方法后当前线程会进入 Compose 主循环并阻塞，
     * 应仅在主线程调用一次；`exitProcessOnExit = false` 用于避免主循环
     * 结束时直接终止进程。
     */
    fun prepare() {
        if (isPrepared) {
            throw IllegalStateException("Can't prepare more than once")
        }
        // 调用此函数后当前线程会陷入 Compose 主循环，需注意调用时机
        // exitProcessOnExit = false 避免主循环结束后进程被直接退出
        application(exitProcessOnExit = false) {
            this@WindowManager.applicationScope = this
            isPrepared = true
            //若存在自定义应用根视图,则将所有窗口显示委托给 `userInsteadApplicationRootContent`, 否则直接显示所有窗口
            userInsteadApplicationRootContent?.Invoke(scope = this, content = { AllWindows() }) ?: this.AllWindows()
        }
    }

    /**
     * 默认根视图内容：渲染所有窗口与无宿主弹窗。
     */
    @Composable
    private fun ApplicationScope.AllWindows() {
        windows.invoke(this)
        dialogs.invoke(Unit)
    }

    /**
     * 移除窗口，触发其 `onDispose` 流程，窗口不再显示。
     *
     * 等价于以下代码中的onCloseRequest实现
     * ```
     * application {
     *     var windowVisible by remember { mutableStateOf(true) }
     *     if (windowVisible) {
     *         Window(onCloseRequest = { windowVisible = false })
     *     }
     * }
     * ```
     *
     * 由 [androidx.compose.desktop.runtime.activity.Activity.finish] 调用，
     * 业务代码一般不需要直接调用。
     *
     * @param window 要移除的窗口根视图
     */
    fun deAttachWindow(window: ActivityRootViewEntity) {
        windows.deAttach(window)
    }

    /**
     * 添加一个待显示的窗口。
     *
     * 若当前尚无可显示的窗口（即 [isEmpty] 为 true），调用方应先确保
     * [prepare] 已启动主循环；[prepare] 由框架在应用启动阶段调用一次。
     *
     * 可从任意线程调用。
     *
     * @param window 要显示的窗口根视图
     */
    @Synchronized
    fun attachWindow(window: ActivityRootViewEntity) {
        windows.attach(window)
    }

    /**
     * 移除指定的无宿主弹窗。
     *
     * @param window 要移除的弹窗根视图
     */
    fun deAttachDialog(window: RootViewEntity<Unit>) {
        dialogs.deAttach(window)
    }

    /**
     * 添加一个待显示的无宿主弹窗。
     *
     * 可从任意线程调用。
     *
     * @param window 要显示的弹窗根视图
     */
    @Synchronized
    fun attachDialog(window: RootViewEntity<Unit>) {
        dialogs.attach(window)
    }

    /**
     * 清空所有窗口与弹窗，触发它们的 `onDispose` 流程。
     *
     * 通常在应用退出前调用；不会终止 Compose `application {}` 主循环，
     * 如需退出请使用 [exitApplication]。
     */
    fun release() {
        windows.clear()
        dialogs.clear()
    }

    /**
     * 退出 Compose `application {}` 主循环，结束应用。
     *
     * 若 [prepare] 尚未调用（[applicationScope] 为 null），此方法不产生任何效果。
     */
    fun exitApplication() {
        applicationScope?.exitApplication()
    }

    /**
     * 当前是否存在已显示的窗口。
     *
     * @return true 表示 [windows] 为空，没有窗口正在显示
     */
    fun isEmpty(): Boolean {
        return windows.isEmpty()
    }

}

/** ApplicationScope 中承载根视图的 Compose 内容。 */
typealias ApplicationComposableContent = @Composable ApplicationScope.() -> Unit

/** [FrameWindowScope] 中承载窗口内容的 Compose 内容。 */
typealias FrameWindowComposableContent = @Composable FrameWindowScope.() -> Unit

/** [DialogWindowScope] 中承载弹窗内容的 Compose 内容。 */
typealias DialogWindowComposableContent = @Composable DialogWindowScope.() -> Unit

/** 无接收者的普通 Compose 内容。 */
typealias ComposableContent = @Composable () -> Unit

/**
 * 应用根视图提供者，用于自定义 Compose `application {}` 中的显示结构。
 *
 * 在默认实现中，[WindowManager] 直接在 `application {}` 中遍历并渲染所有窗口，
 * 用户无法为要显示的窗口（框架内窗口或者说Activity根视图）提供提供[CompositionLocal]值、主题、异常捕获等外层结构，也无法实现显示脱离框架的窗口。
 * 因此提供此接口用于实现上述无法实现之事，由用户实现application根视图结构，提供自定义显示内容。
 *
 * 用法：
 * ```
 * object : ApplicationRootContent {
 *    // 框架内要显示的所有窗口将作为windows参数传递
 *     override fun ApplicationScope.invoke(windows: ComposableContent) {
 *         UncaughtExceptionContent {
 *             MaterialTheme {
 *                 windows() // 必须调用，否则所有框架内的窗口不会显示
 *                 SystemTray()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 */
fun interface ApplicationRootContent {

    /**
     *
     * 此方法将在[WindowManager]中由框架调用，渲染 Application 根视图。
     *
     * **实现契约**：必须调用 [windows]，否则 [WindowManager] 中挂载的任何窗口
     * 都不会显示；其它内容（主题、托盘、异常包装等）可自由组合。
     *
     * @param windows 承载所有窗口的 Compose 函数，必须调用。
     * @receiver 当前 Compose `application {}` 的 [ApplicationScope]
     */
    @Composable
    operator fun ApplicationScope.invoke(windows: ComposableContent)
}

/**
 * 提供在ApplicationRootContent之外更简单的调用[ApplicationRootContent.invoke]方法能力
 *
 * @param scope   当前 [ApplicationScope]
 * @param content 承载所有窗口的 Compose 内容
 */
@Composable
internal fun ApplicationRootContent.Invoke(scope: ApplicationScope, content: ComposableContent) =
    scope.invoke(content)
