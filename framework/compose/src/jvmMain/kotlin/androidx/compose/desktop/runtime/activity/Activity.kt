@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.desktop.runtime.activity

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.core.context.Context
import androidx.compose.desktop.runtime.core.context.LocalContext
import androidx.compose.desktop.runtime.core.context.ThemedContext
import androidx.compose.desktop.runtime.core.intent.Intent
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
import androidx.compose.desktop.runtime.savestate.Token
import androidx.compose.desktop.runtime.utils.UncaughtExceptionContent
import androidx.compose.desktop.runtime.utils.WeakReferenceDelegate
import androidx.compose.desktop.runtime.utils.setUncaughtExceptionHandler
import androidx.compose.desktop.runtime.window.ActivityRootViewEntity
import androidx.compose.desktop.runtime.window.ApplicationComposableContent
import androidx.compose.desktop.runtime.window.ApplicationScopeToken
import androidx.compose.desktop.runtime.window.RootViewEntity
import androidx.compose.desktop.runtime.window.RootViewMgr
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.application
import androidx.core.bundle.Bundle
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.jvm.system.di.inject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread
import androidx.compose.ui.window.Window

/**
 * Activity 是 Compose Desktop 中的窗口容器，负责：
 * 1. 承载 Compose 窗口内容并链接 [ComposeWindow] 的生命周期
 * 2. 通过 [Intent] 接收启动参数、回传结果
 * 3. 结合 [ApplicationSaveStateSaver] 实现状态保存与恢复
 *
 * ## 生命周期
 *
 * Activity 自身不产生生命周期事件，而是**同步 [ComposeWindow] 的生命周期**：
 *
 * | Swing listener callbacks     | Lifecycle event | Lifecycle state change |
 * |------------------------------|-----------------|------------------------|
 * | windowIconified(最小化)              | ON_STOP         | STARTED → CREATED      |
 * | windowDeiconified(还原)             | ON_START        | CREATED → STARTED      |
 * | windowLostFocus(失去焦点、隐藏)       | ON_PAUSE        | RESUMED → STARTED      |
 * | windowGainedFocus(获得焦点、恢复显示) | ON_RESUME       | STARTED → RESUMED      |
 * | dispose(移除window)                 | ON_DESTROY      | CREATED → DESTROYED    |
 *
 * 注意：自 androidx.lifecycle 2.9.0-alpha06 起，[Lifecycle.DESTROYED] 为终态，
 * 任何从该状态向其他状态的迁移都会抛出 [IllegalStateException]。
 *
 * 实现一个窗口显示需在`application {}`中调用[Window]
 * ```
 * application {
 *     var windowVisible by remember { mutableStateOf(true) }
 *     if (windowVisible) {
 *         Window(onCloseRequest = { windowVisible = false })
 *     }
 * }
 * ```
 *
 * Window函数被调用，内部会创建[androidx.compose.ui.awt.SwingWindow]并显示compose内容，SwingWindow的生命周期开始。
 * Window函数不再被调用，从重组树上移除，Window内部触发DisposeEffect，SwingWindow销毁，compose内容不再显示，生命周期结束。
 * 窗口的显示和关闭依靠Window函数从重组树上加载和移除，这也是为什么Window函数提供onCloseRequest参数。
 * 在框架的窗口管理实现中，显示和关闭窗口也依赖此原理。 查看[finish]
 *
 * ## 启动与结果回传
 *
 * 启动 Activity 的方式：
 * 1. 通过 [Context] 的 `startActivity` / `startActivityForResult`
 * 2. 通过 `IActivityLauncher` 或 `IntentProcessor` 服务
 * 3. 通过 `ActivityManager`
 *
 * 结果回传既可使用 `startActivityForResult` 的回调（其内部是对[Intent.activityResultFlow] 的封装），也可直接订阅该 Flow。
 * 若需自定义通道，可使用 [Intent.getMailBox]
 *
 * ## 用法
 * * 示例Activity
 * ```
 * class ExampleActivity : Activity() {
 *     override fun onCreate(savedInstanceState: SavedState?) {
 *         super.onCreate(savedInstanceState)
 *         setContent {
 *             Window(onCloseRequest = { hide() }, visible = mVisibility) {
 *                 LinkWindow { MaterialTheme { /* content */ } }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * * 启动activity并获取结果
 *
 * 示例：
 * 1. 在MainActivity中启动详情页面并获取结果
 * ```
 * val intent = Intent(this@MainActivity,DetailActivity::class.java).apply {
 *     multiApplication = true
 *     token = Token("detail-1")
 *     data {
 *         putInt("param1", 1024)
 *         putString("param2", "str")
 *     }
 * }
 * scope.launch {
 *     startActivityForResult(intent) { result, data ->
 *         vmActivityResult = data.toString()
 *         logger.info("data: $data")
 *     }
 * }
 * ```
 * 2. 在详情页面设置结果并关闭页面
 * ```
 * //获取启动参数
 * val params1 = intent?.getData<Int>("param1")
 * 或者
 * val params1 = intent?.mData.get<Int>("param1")
 *
 * //设置结果并关闭
 * setResult(Activity.SUCCESS, bundleOf("result" to value))
 * finish()
 * ```
 *
 * * 启动activity除了使用context中的方法，还可以：
 * 1. 使用ActivityManager [ActivityManager.startActivity]
 * 2. 获取ActivityLauncher 启动activity
 *  ```
 *  在context中
 *  getService<IActivityLauncher>(IActivityLauncher::class).start(intent)
 *  在任意地方
 *  ServiceBooter.getService<IActivityLauncher>(IActivityLauncher::class).start(intent)
 *  ```
 * 3. 获取IntentProcessor 启动activity
 *  ```
 *  在context中
 *  getService<IntentProcessor>(IntentProcessor::class).start(intent)
 *  在任意地方
 *  ServiceBooter.getService<IntentProcessor>(IntentProcessor::class).start(intent)
 *  ```
 *
 * * 除了上面使用startActivityForResult，通过回调接口获取结果外，还可以从intent中的activityResultFlow中collect结果
 *
 * 实际上，startActivityForResult方法回调接口就是封装自intent中的activityResultFlow
 *
 * ```
 * intent.activityResultFlow.collect { result ->
 *
 * }
 * ```
 *
 * 除了使用预定义的activityResultFlow，还可以设定自定义的信箱用于两个activity之间的数据传递
 * ```
 * activity1 启动 activity2,获取一个MutableSharedFlow观察activity2回传的结果
 * intent.getMailBox<Int>("id").collect {
 *      //.....
 * }
 *
 * activity2处理完成后使用MutableSharedFlow回传结果
 * intent?.getMailBox<Int>("id").emit(10)
 * ```
 *
 * @see Intent
 * @see ApplicationSaveStateSaver
 * @see LinkWindow
 * @see ActivityManager
 */
abstract class Activity : ThemedContext(), LifecycleOwner, InstanceKoinComponent {
    private val logger = LoggerFactory.getLogger(this.toString())

    /**
     * 可选的生命周期监听器，由子类设置，用于监听 Window 的生命周期变化。
     * 为 null 时表示子类不关心窗口生命周期。
     */
    protected var lifecycleListener: LifecycleEventObserver? = null

    /**
     * 观察 [ComposeWindow] 的生命周期并同步到本 Activity。
     *
     * 注意：
     * - ComposeWindow 的 [ON_CREATE] 不同步：Activity 进入 onCreate 后
     *   才会显示 ComposeWindow，此时 Activity 已处于 CREATED 状态。
     * - ComposeWindow 销毁时移除监听，并结束 Activity 生命周期。
     */
    internal val parentLifecycleObserver = object : LifecycleEventObserver {
        /**
         * 观察window的生命周期，并进行同步
         * 当window销毁时，activity的生命周期结束
         */
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            lifecycleListener?.onStateChanged(source, event)
//            logger.info("window lifecycle event: $event")
            if (event != Lifecycle.Event.ON_CREATE) {
                syncLife(event)
            }
            when (event) {
                ON_RESUME -> onResume()
                ON_PAUSE -> onPause()
                ON_STOP -> onStop()
                ON_DESTROY -> {
                    // 窗口已销毁，Activity 随之关闭，无需继续监听
                    source.lifecycle.removeObserver(this)
                    onDestroy()
                }

                ON_START -> onStart()
                else -> {}
            }
        }
    }

    val stateSaver by inject<ApplicationSaveStateSaver>()

    /** 启动本 Activity 的 [Intent]，通过弱引用持有。 */
    var intent by WeakReferenceDelegate<Intent>()

    @Suppress("LeakingThis")
    protected var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this@Activity)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    /**
     * 每个 Activity 的唯一标识，用于关联状态保存与恢复。
     *
     * 若 [intent] 携带 [Token]，则以该 Token 作为标识并启用状态保存/恢复；
     * 否则使用基于类名的默认 Token，此时不启用状态保存/恢复。
     */
    internal val token: Token?
        get() = intent?.token

    private val finalId = Token(this::class.qualifiedName ?: this::class.hashCode().toString())

    /**
     * Activity 的稳定唯一标识。
     *
     * 优先使用 [token]；当 [token] 为 null（未启用状态保存/恢复）时，
     * 退化为基于类名的 [finalId]，以保证标识始终非空。
     */
    protected val idn: Token get() = token ?: finalId

    /**
     * 当前 Activity 的状态容器。
     *
     * 状态来源包括 [onSaveInstanceState]、[SavedStateRegistry] 等。
     * 仅当 [token] 不为 null（即启用了状态保存）时才有值，否则返回 null。
     */
    internal val savedState: SavedState?
        get() {
            val id = token ?: return null
            return stateSaver.obtain(id)
        }

    val context get() = this

    /** 仅作为 [finish] 是否已被调用的标志，避免重复关闭。 */
    private var finished: Boolean = false

    /**
     * 单 Application 模式下的根视图。
     * 多 Application 模式下请使用 [multiApplicationToken]。
     */
    internal var rootViewEntity: ActivityRootViewEntity = ActivityRootViewEntity()

    /**
     * 当前 Activity 上挂载的模态 Dialog 根视图集合。
     *
     * 若 ComponentDialog 配置为模态窗口，显示时会把其根布局保存在此，
     * 由宿主 Activity 的 Compose 作用域重组显示弹窗。
     */
    internal val dialogsMgr = RootViewMgr<Unit>()

    /**
     * 当前 [ComposeWindow] 实例，由 [LinkWindow] 赋值。
     * 仅在窗口已显示时非 null。
     */
    var composeWindow: ComposeWindow? = null
        internal set

    /**
     * 多 Application 特性相关令牌。
     *
     * 启用后 Activity 会使用独立的 applicationScope 显示根视图，
     * 而非挂载到全局 WindowManager；调用 [finish] 时会连同独立
     * application 一并销毁。
     */
    val multiApplicationToken: ApplicationScopeToken = ApplicationScopeToken(null)

    /**
     * 窗口可见性，供 [androidx.compose.ui.window.Window] 的 `visible` 参数使用。
     * 需在 [setContent] 的 Window 中绑定此值，[show]/[hide] 才会生效。
     */
    var mVisibility by mutableStateOf(true)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    /** 显示窗口（将 [mVisibility] 置为 true）。 */
    fun show() {
        mVisibility = true
    }

    /** 隐藏窗口（将 [mVisibility] 置为 false），生命周期进入 [ON_PAUSE]。 */
    fun hide() {
        mVisibility = false
    }

    /**
     * 将 Activity 附加到运行环境：
     * 1. 注册到 ActivityManager；
     * 2. 设置基础 [Context]；
     * 3. 进入 [onCreate] 并开始生命周期流程。
     *
     * 由框架调用，业务代码不应直接调用。
     *
     * @param context 基础上下文
     * @param intent  启动本 Activity 的 Intent
     */
    internal fun attach(
        context: Context,
        intent: Intent,
    ) {
        this.intent = intent
        attachBaseContext(context)
        // activityManager()调用前必须先attach context, 否则因为初始化报错
        activityManager().register(idn, this@Activity)
        onCreate(savedState)
    }

    /**
     * 将 Activity 的生命周期状态同步到 [event] 的目标状态。
     *
     * @param event 需要同步的生命周期事件
     */
    private fun syncLife(event: Lifecycle.Event) {
        lifecycleRegistry.currentState = event.targetState
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    /**
     * 首次创建时回调，在此调用 [setContent] 显示界面。
     * ```
     * override fun onCreate(savedInstanceState: SavedState?) {
     *     super.onCreate(savedInstanceState)
     *     setContent {
     *         Window(
     *             onCloseRequest = { hide() },
     *             visible = mVisibility,
     *         ) {
     *             LinkWindow {
     *                MaterialTheme{}
     *             }
     *         }
     *     }
     * }
     * ```
     * @param savedInstanceState 上次保存的状态，首次创建时为 null
     */
    @CallSuper
    open fun onCreate(savedInstanceState: SavedState?) {
        this.syncLife(ON_CREATE)
    }

    /**
     * 保存实例状态，将在 [onDestroy] 前被调用。
     *
     * @param outState 用于写入待保存状态的容器
     */
    @CallSuper
    open fun onSaveInstanceState(outState: SavedState) {

    }

    open fun onStart() {}

    /**
     * 设置Activity根视图，需在 [onCreate] 中调用。
     * 在此方法的content参数实现中必须调用[Window]才可显示窗口。
     * 需要在[Window]中调用 [LinkWindow] 以建立与 [ComposeWindow] 生命周期的关联。
     *
     * 默认通过 WindowManager 的全局 application 显示；
     * 若 [Intent.multiApplication] 为 true，则使用独立 application 显示。
     *
     * @param content 根视图，需内部调用 [androidx.compose.ui.window.Window]
     *
     * 用法：
     * @see onCreate
     */
    protected open fun setContent(content: ApplicationComposableContent) {
        //如果使用多Application特性,则启动单独的application显示窗口
        if (intent?.multiApplication == true) {
            val thread = thread {
                setUncaughtExceptionHandler()
                try {
                    application(
                        exitProcessOnExit = false,
                        content = {
                            if (!multiApplicationToken.destroy) {
                                UncaughtExceptionContent {
                                    content()
                                }
                            }
                        }
                    )
                } catch (ignore: InterruptedException) {
                    // 线程被中断属于正常退出路径，忽略
                }
            }
            multiApplicationToken.thread = thread
        } else {
            // 未启用多 Application：将根视图放入 WindowManager 的可观察列表，
            // 触发全局 ApplicationScope 重组并显示此 Activity 根视图/窗口
            this.rootViewEntity.rootContent = content
            windowManager().attachWindow(this@Activity.rootViewEntity)
        }
    }

    /**
     * 将当前 Activity 与所在 [ComposeWindow] 的生命周期绑定。
     *
     * 必须在 [androidx.compose.ui.window.Window] 的 `content` 中调用。
     * 调用后会：
     * - 注册 [parentLifecycleObserver] 以同步窗口生命周期；
     * - 记录当前 [composeWindow]；
     * - 提供 [LocalContext] 与 [ActivityLifecycleOwner]；
     * - 渲染通过 [attachDialog] 挂载的弹窗。
     *
     * @param content 窗口内容
     *
     * 用法：
     * @see onCreate
     */
    @Composable
    protected open fun FrameWindowScope.LinkWindow(content: @Composable FrameWindowScope.() -> Unit) {
        //这里的lifecycle是composeContainer的提供的
        val lc: LifecycleOwner = LocalLifecycleOwner.current
        remember {
            lc.lifecycle.addObserver(parentLifecycleObserver)
        }
        this@Activity.composeWindow = this.window
        CompositionLocalProvider(
            LocalContext provides context,
            ActivityLifecycleOwner provides this@Activity,
        ) {
            content()
        }
        // 渲染通过 attachDialog 挂载的弹窗
        dialogsMgr.invoke(Unit)
    }

    /**
     * 卸载指定的 Dialog 根视图。
     */
    fun deAttachDialog(window: RootViewEntity<Unit>) {
        dialogsMgr.deAttach(window)
    }

    /**
     * 挂载一个待显示的 Dialog 根视图。
     */
    @Synchronized
    fun attachDialog(window: RootViewEntity<Unit>) {
        dialogsMgr.attach(window)
    }

    /**
     * 单例模式下，再次通过 Intent 启动本 Activity 时回调。
     *
     * 此回调不会改变 Window 或 Activity 的生命周期状态。
     *
     * @param intent 新的启动 Intent，可能为 null
     */
    @CallSuper
    open fun onReStart(intent: Intent? = null) {

    }

    @CallSuper
    open fun onPause() {
    }

    @CallSuper
    open fun onResume() {
    }

    @CallSuper
    open fun onStop() {
    }

    /**
     * 由窗口的生命周期同步触发，业务代码不应主动调用。
     *
     * 触发流程与功能
     * 1. [finish] 将根视图从 WindowManager 移除，ApplicationScope 重组；
     * 2. 承载 [ComposeWindow] 的根内容从重组树移除，触发 onDispose；
     * 3. ComposeWindow 进入 [ON_DESTROY]，Activity 同步进入 [ON_DESTROY]；
     * 4. 清理 ActivityManager 中的注册信息与弹窗。
     */
    @CallSuper
    open fun onDestroy() {
        savedState?.let { onSaveInstanceState(it) }
        finished = true
        activityManager().remove(idn)
        dialogsMgr.clear()
    }

    /**
     * 关闭当前 Activity 并触发 [ON_DESTROY]。
     * 此方法会将窗口函数从重组树上移除，使窗口结束生命周期。
     *
     * 通常在 [androidx.compose.ui.window.Window] 的 `onCloseRequest`中调用，
     * 或者任何需要关闭窗口的地方。
     * ```
     * Window(onCloseRequest = { finish() }, visible = mVisibility) { ... }
     * ```
     *
     * 处理逻辑：
     * - 单 Application 且窗口已显示：从 WindowManager 移除根视图
     * - 多 Application 且窗口已显示：销毁独立 application
     * - 没有界面：直接将生命周期同步至 [ON_DESTROY]
     */
    @CallSuper
    open fun finish() {
        if (rootViewEntity.isAttached) {
            // 单 Application 下已显示窗口
            windowManager().deAttachWindow(rootViewEntity)
            composeWindow = null
        } else {
            // 多 Application 特性启用且已显示窗口
            if (multiApplicationToken.isExist) {
                multiApplicationToken.dismiss()
            } else {
                // 没有界面，直接将生命周期同步到 ON_DESTROY
                syncLife(ON_DESTROY)
                onDestroy()
            }
        }
    }

    //<editor-fold desc="result callback">

    /**
     * 结果流，来自 [intent] 中的默认信箱 [DEFAULT_RESULT_FLOW]。
     *
     * @throws IllegalStateException 当 intent 为 null 或未提供结果信箱时抛出
     */
    internal val internalResultFlow
        get() = intent?.getMailBox<ActivityResult>(DEFAULT_RESULT_FLOW)
            ?: throw IllegalStateException("activity_result_flow can't be null")

    /**
     * 设置返回给启动方的结果。
     *
     * 结果通过 [Intent.activityResultFlow] 发送，`startActivityForResult`的回调即订阅此流。
     *
     * @param resultCode 结果码，[SUCCESS] 或 [FAILED]
     * @param data       附加数据，可为 null
     */
    open fun setResult(resultCode: Int, data: Bundle? = null) {
        internalResultFlow.tryEmit(ActivityResult(resultCode, data))
    }

    //</editor-fold>

    companion object {
        /** 默认结果信箱的 key。 */
        const val DEFAULT_RESULT_FLOW = "activity_result_flow"

        /** 成功结果码。 */
        const val SUCCESS = 1

        /** 失败结果码。 */
        const val FAILED = 0
    }
}
