@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.desktop.runtime.activity

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.core.context.Context
import androidx.compose.desktop.runtime.core.context.LocalContext
import androidx.compose.desktop.runtime.core.context.ThemedContext
import androidx.compose.desktop.runtime.core.intent.Intent
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
import androidx.compose.desktop.runtime.savestate.Token
import androidx.compose.desktop.runtime.savestate.WeakReferenceDelegate
import androidx.compose.desktop.runtime.utils.UncaughtExceptionContent
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
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.application
import androidx.core.bundle.bundleOf
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

/**
 * androidx lifecycle 2.9.0-alpha06 Lifecycle.DESTROYED 状态是最终状态，现在，如果尝试将
 * Lifecycle 从该状态移至任何其他状态，都会导致 IllegalStateException。
 *
 * 当隐藏window，生命周期会走到[ON_PAUSE]
 * 当window被移除（调用[finish]方法、手动点击窗口关闭按钮），window生命周期会走到[ON_DESTROY]，
 * 我们本就实现了隐藏与显示方法，根本不需要activity在关闭windows后重新生成window来显示界面，重走生命周期，
 * 而且当前生命走到[ON_DESTROY]时是无法设置其他生命周期状态的，因此，activity理应同步window生命周期
 *
 * 如下是jb对于window的生命周期描述。
 *
 * | Swing listener callbacks     | Lifecycle event | Lifecycle state change |
 * |------------------------------|-----------------|------------------------|
 * | windowIconified(最小化)         | ON_STOP         | STARTED → CREATED      |
 * | windowDeiconified(还原)        | ON_START        | CREATED → STARTED      |
 * | windowLostFocus(失去焦点、隐藏)     | ON_PAUSE        | RESUMED → STARTED      |
 * | windowGainedFocus(获得焦点、恢复显示) | ON_RESUME       | STARTED → RESUMED      |
 * | dispose(移除window)            | ON_DESTROY      | CREATED → DESTROYED    |
 *
 * activity的生命周期并不完全与window同步.
 *
 * [Activity.finish] - 关闭窗口，不可恢复，标志着生命周期走到[ON_DESTROY] [Activity.hide] -
 * 隐藏窗口，可以恢复，标记生命周期走到[ON_PAUSE]
 *
 * compose resource目前可以使用多国语言，但是它不给你动态修改的功能，相关类和方法都是internal的。
 * 但是，它的功能实现实际上依赖于Java.Locale，因此我们可以通过在compose刷新之前修改Java.Locale，
 * 从而半支持多国语言的动态切换（这需要触发整个页面compose的重绘）。
 *
 * 首先，修改java默认locale，然后关闭窗口，此时compose进入onStop状态，
 * 重新打开窗口，compose重加载，重新读取了Java.Locale，从而语言得到了修改。
 *
 *
 * 通常我们会这么使用Window
 * ```
 * application {
 *     var windowClosed by remember { mutableStateOf(false) }
 *     var mVisible by remember { mutableStateOf(true) }
 *     if (!windowClosed) {
 *         Window(
 *             onCloseRequest = { windowClosed = true },
 *             isVisible = mVisible,
 *         ) {
 *              //界面
 *         }
 *     }
 * }
 * ```
 * 当windowClosed为false时, Window会挂载到重组树,内部创建ComposeWindow, 并将显示为窗口,并显示Compose界面
 * 当关闭窗口时,会触发onCloseRequest回调, 你需要将windowClosed置为true, 这样Window会从重组树上卸载,不再显示窗口
 *
 *  原理:
 * Window函数会创建ComposeWindow,让其显示Compose视图
 * ComposeWindow 继承自JFrame, 内部会添加一个能渲染Compose视图的JPanel,
 * 这个JPanel会给JFrame添加状态监听,并将其转换为生命周期,使用LocalLifecycle提供给要显示的compose视图
 * 因此, ComposeWindow生命周期会一半跟随compose视图状态, 窗口显示时生命周期走到ON_CREATE并显示compose视图,
 * compose视图不再显示时触发onDispose,窗口取消显示,生命周期走到ON_DESTROY
 * 显示JFrame时需要将JFrame.isVisible 赋值为true; 不再显示JFrame时需要将JFrame.isVisible 赋值为false,
 *
 * Window中的伪代码如下:
 * ```
 *
 * fun Window(content: @Composable ()->Unit ) {
 *     val window = remember{
 *         ComposeWindow(content).apply{ //一开始就创建JFrame并加载compse视图, 生命周期走到ON_CREATE
 *             isVisible = true
 *         }
 *     }
 *     DisposableEffect(){
 *         onDispose {//compose视图不再显示, 取消显示JFrame, 此时生命周期走到ON_DESTROY
 *             window.isVisible = false
 *         }
 *     }
 * }
 *
 * class ComposeWindow(val content: @Composable ()->Unit) :JFrame{
 *     init{
 *         addPanel(ComposePanel(content,this))
 *     }
 * }
 *
 *
 * class ComposePanel(val content: @Composable ()->Unit, val jFrame:JFrame): JPanel{
 *     init{
 *         jFrame.addStateListener{ state ->
 *             syncLifecycle(state.toLifecycleEvent())
 *         }
 *         ...经过一些复杂处理,显示compose内容, 当然这里只是伪代码, 实际情况不会在这里调用content
 *         LocalLifecycle.Provide(...){
 *             content()
 *         }
 *     }
 *     fun syncLifecycle(state:Lifecycle.Event){
 *         //将生命周期设置到LocalLifecycle
 *     }
 * }
 * ```
 *
 * 因此,
 * 1. 当windowClosed为false时调用了Window函数, 内部创建JFrame显示窗口, 显示compose视图.
 * 2. 当点击窗口的关闭按钮, 回调Window的onCloseRequest, 将windowClosed置为true,
 *   Window从重组树上卸载, 触发DisposableEffect,将JFrame的isVisible置为false, JFrame不再显示, 且ComposeWindow生命周期走到ON_DESTROY
 *
 *
 * 在具体实现窗口管理中, 点击关闭按钮触发Window的onCloseRequest, 此时需要将视图内容从ApplicationScope中卸载,
 * 不点击关闭按钮,从程序逻辑中关闭窗口,其实也是将视图内容从ApplicationScope中卸载, 流程是一致的.
 * 也就是直接调用finish即可.
 * 其实总结起来也会发现,窗口关闭的整个过程是先将Window从重组树上移除, 然后ComposeWindow才会触发ON_DESTROY的生命周期事件,
 * 而不是先触发ComposeWindow的ON_DESTROY的生命周期事件,再将Window从重组树上移除.
 */
abstract class Activity : ThemedContext(), LifecycleOwner, InstanceKoinComponent {
    private val logger = LoggerFactory.getLogger(this.toString())

    /**
     * 子类可以指定此实例用于监听Window的生命周期变化
     */
    protected var lifecycleListener: LifecycleEventObserver? = null
    internal val parentLifecycleObserver = object : LifecycleEventObserver {
        /**
         * 观察window的生命周期，并进行同步
         * 当window销毁时，activity的生命周期结束
         */
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            lifecycleListener?.onStateChanged(source, event)
//            logger.info("window lifecycle event: $event")
            /*
            * 有些生命周期事件不需要同步
            * 1. onCreate状态: activity生成实例后会被调用attach方法,开始生命周期流程并进入onCreate状态,此后会显示compose window并同步compose window的生命周期状态.
            * 因此,当同步compose window生命周期状态的时候,activity已经进入了onCreate状态,根本不需要同步compose window的onCreate状态.
            */
            if (event != Lifecycle.Event.ON_CREATE) {
                syncLife(event)
            }
            when (event) {
                ON_RESUME -> onResume()
                ON_PAUSE -> onPause()
                ON_STOP -> onStop()
                ON_DESTROY -> {
                    //当窗口生命周期走到onDestroy状态,activity也就此关闭,因此不要再继续监听窗口的生命周期
                    source.lifecycle.removeObserver(this)
                    onDestroy()
                }

                ON_START -> onStart()
                // on_create事件不需要同步
                else -> {}
            }
        }
    }

    val stateSaver by inject<ApplicationSaveStateSaver>()
    var intent by WeakReferenceDelegate<Intent>()

    @Suppress("LeakingThis")
    protected var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this@Activity)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    /**
     * 每个activity都有唯一的token,也就是id
     * 使用此id关联保存的状态,以便下次启动后恢复状态
     * 如果此id为null,则不使用状态保存和恢复功能
     */
    internal val token: Token?
        get() = intent?.token

    private val finalId = Token(this::class.qualifiedName ?: this::class.hashCode().toString())

    /**
     * 上面的token与状态保存和恢复相关,如果不需要使用状态保存和恢复功能,则token为null,
     * 此时就无法使用token标识activity的唯一性了,因此需要一个回退字段标识唯一性.
     */
    protected val idn: Token get() = token ?: finalId

    /**
     * 在[ApplicationSaveStateSaver]中使用token注册一个SaveState,用于存放所有需要保存的状态
     * 状态栏会来自[onSaveInstanceState]、[SavedStateRegistry]等
     * 调用此方法时需要确保已经给intent赋过值
     */
    internal val savedState: SavedState?
        get() {
            val id = token ?: return null
            return stateSaver.getSaveState(id)
        }

    /**
     * activity实现了IContext接口
     */
    val context get() = this

    /**
     * 仅作为一个调用[finish]方法的标志
     */
    private var finished: Boolean = false

    /**
     * 根视图,仅在单Application模式下有用
     */
    internal var rootViewEntity: ActivityRootViewEntity = ActivityRootViewEntity()

    /**
     * 如果ComponentDialog配置为模态窗口,
     * 则显示时会将ComponentDialog根布局插入到其宿主Activity的dialogsSlots中
     */
    internal val dialogsMgr = RootViewMgr<Unit>()

    /**
     * 调用[LinkComposeWindow]后,此变量用于记录当前的ComposeWindow
     */
    var composeWindow: ComposeWindow? = null
        internal set

    /**
     * 启用多Application特性启动Activity, 会使用独立的applicationScope显示根视图,
     * 而不会将根视图放入WindowManager使用全局applicationScope显示.
     * 你依旧可以使用finish结束此Activity,会连同独立的application一并销毁.
     *
     * 此变量记录启动独立application信息
     */
    val multiApplicationToken: ApplicationScopeToken = ApplicationScopeToken(null)

    /**
     * 在实现类中需调用Window,将Window中的visible参数指定为此变量才可生效
     */
    var mVisibility by mutableStateOf(true)
    fun show() {
        mVisibility = true
    }

    fun hide() {
        mVisibility = false
    }

    /**
     * 1. activity将自己注册进[ActivityManager]
     * 2. 开始自己的生命周期
     * 3. 生成mWindow，并调用[onCreate]方法
     */
    internal fun attach(
        context: Context,
        intent: Intent,
    ) {
        this.intent = intent
        attachBaseContext(context)
        activityManager().register(idn, this@Activity)
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        onCreate(savedState)
    }

    /**
     * 观察Window的生命周期，并同步给activity的[lifecycleRegistry]
     * 但是，不能同步[ON_DESTROY]状态，因为activity的生命周期理应比window更长。
     *
     * @param event 需要同步的生命周期事件
     */
    private fun syncLife(event: Lifecycle.Event) {
        lifecycleRegistry.currentState = event.targetState
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    /**
     * Called when the activity is first created.
     *
     * @param data 启动此Activity附带的数据
     */
    @CallSuper
    open fun onCreate(savedInstanceState: SavedState?) {
        this.syncLife(ON_CREATE)
    }

    @CallSuper
    open fun onSaveInstanceState(outState: SavedState) {

    }

    open fun onStart() {}

    /**
     * 在子类onCreate函数中调用, 以显示窗口界面.
     * 在此函数传入content内部需要调用[androidx.compose.ui.window.Window]才可使Activity显示窗口.
     * 在调用Window函数传入的content内部需调用[LinkComposeWindow]才可使Activity链接ComposeWindow生命周期.
     * ```
     * open class MainActivity : Activity() {
     *     override fun onCreate(savedInstanceState: SavedState?) {
     *         super.onCreate(savedInstanceState)
     *         setContent {
     *              Window(
     *                  onCloseRequest = { finish() },
     *                  visible = mVisibility,
     *              ) {
     *                  Link2ComposeWindow {
     *                      //界面
     *                  }
     *              }
     *         }
     *     }
     * }
     * ```
     *
     * @param content 根视图
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

                }
            }
            multiApplicationToken.thread = thread
        } else {
            // 如果未使用多application特性, 将根视图放入WindowManager的可观察列表,
            // 促使全局ApplicationScope重组并显示此Activity根视图/窗口
            this.rootViewEntity.rootContent = content
            windowManager().attachWindow(this@Activity.rootViewEntity)
        }
    }

    /**
     * 1. 使Activity链接ComposeWindow生命周期
     * 2. 向Compose子视图提供ViewModelStoreOwner、SaveStateRegister、SaveableStateRegister等组件
     */
    @Composable
    protected open fun FrameWindowScope.LinkComposeWindow(content: @Composable FrameWindowScope.() -> Unit) {
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
        //显示添加到Activity的弹窗
        dialogsMgr.invoke(Unit)
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

    /**
     * 当为单例模式时，再次启动activity将回调此方法。
     *
     * 但是此方法被触发之后，不会改变window或者activity的生命周期状态。
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
     * ```
     * Window(
     *  onCloseRequest = { finish() },
     *  visible = mVisibility,
     * )
     * ```
     *
     * 1. 手动点击窗口的"X"关闭按钮, Window触发onCloseRequest回调, 调用finish函数
     * 2. 直接调用finish函数
     * 会触发如下流程：
     * 调用WindowManager的deAttachWindow将rootContent移除，applicationScope重组，
     * 承载着ComposeWindow的rootContent从重组树上被删除，不再显示。
     * rootContent内部的ComposeWindow触发onDispose流程，ComposeWindow生命周期走到ON_DESTROY状态，
     * 由于activity同步ComposeWindow的生命周期，于是activity也会进入[ON_DESTROY]状态，
     * 并调用[onDestroy]方法, 移除WindowManager中注册的compose视图.
     *
     * 生命周期流程:
     * activity -> observe and sync -> window lifecycle
     * 即
     * close application window -> window lifecycle update to ON_DESTROY
     * -> activity sync window lifecycle -> activity lifecycle will set to ON_DESTROY and invoke onDestroy function
     *
     * 注:
     *  1. 如果ComposeWindow已显示,则从WindowManager中移除compose视图(此视图函数中调用了Window函数)
     *  2. 如果ComposeWindow未显示,则直接使生命周期进入ON_DESTROY
     */
    @CallSuper
    open fun onDestroy() {
        savedState?.let { onSaveInstanceState(it) }
        finished = true
        activityManager().remove(idn)
        dialogsMgr.clear()
    }

    /**
     * 点击窗口的 X 按钮,会触发Window的onCloseRequest回调, 在onCloseRequest中请调用finish方法,
     *
     * 此方法将rootContent(内部会调用Window函数,使Activity追踪ComposeWindow生命周期)从WindowManager移除,
     * 触发ApplicationScope重组,从而将承载Window的rootContent从重组树上移除，
     * 触发ComposeWindow的onDispose流程
     *
     * ```
     * Window(
     *  onCloseRequest = { finish() },
     *  visible = mVisibility,
     * )
     * ```
     */
    @CallSuper
    open fun finish() {
        if (rootViewEntity.isAttached) {
            //单Application下显示了窗口
            windowManager().deAttachWindow(rootViewEntity)
            composeWindow = null
        } else {
            //多application特性不仅启用了, 还显示了窗口界面
            if (multiApplicationToken.isExist) {
                multiApplicationToken.dismiss()
            } else {
                //没有界面,直接将生命周期同步到ON_DESTROY
                syncLife(ON_DESTROY)
                onDestroy()
            }
        }
    }

    //<editor-fold desc="result callback">
    internal val internalResultFlow
        get() = intent?.getMailBox<ActivityResult>(RESULT_FLOW)
            ?: throw IllegalStateException("activity_result_flow can't be null")

    /**
     * @param resultCode 结果码，[Activity.SUCCESS]表示成功，[Activity.FAILED]表示失败
     */
    open fun setResult(resultCode: Int, data: Any) {
        internalResultFlow.tryEmit(ActivityResult(resultCode, bundleOf("data" to data)))
    }

    /**
     * @param resultCode 结果码，[Activity.SUCCESS]表示成功，[Activity.FAILED]表示失败
     */
    open fun setResult(resultCode: Int) {
        internalResultFlow.tryEmit(ActivityResult(resultCode, null))
    }

    //</editor-fold>

    companion object {
        const val RESULT_FLOW = "activity_result_flow"
        const val SUCCESS = 1
        const val FAILED = 0
    }
}
