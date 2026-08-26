@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.desktop.runtime.activity

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.context.Context
import androidx.compose.desktop.runtime.context.ThemedContext
import androidx.compose.desktop.runtime.core.intent.Intent
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
import androidx.compose.desktop.runtime.savestate.Token
import androidx.compose.desktop.runtime.savestate.WeakReferenceDelegate
import androidx.compose.desktop.runtime.window.ActivityContentEntity
import androidx.compose.desktop.runtime.window.ApplicationComposableContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
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
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

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
 */
abstract class Activity : ThemedContext(), LifecycleOwner, InstanceKoinComponent {
    private val logger = LoggerFactory.getLogger(this.toString())
    protected val parentLifecycleObserver = object : LifecycleEventObserver {
        /**
         * 观察window的生命周期，并进行同步
         * 当window销毁时，activity的生命周期结束
         */
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            logger.info("window lifecycle event: $event")
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
                // on_create事件不需要同步,
                ON_CREATE -> logger.info("compose window new lifecycle state:onCreate. ignore")
                ON_ANY -> logger.info("compose window new lifecycle state:ON_ANY. ignore")
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

    private val finalId = Token(this::class.qualifiedName?:this::class.hashCode().toString())

    /**
     * 上面的token与状态保存和恢复相关,如果不需要使用状态保存和恢复功能,则token为null,
     * 此时就无法使用token标识activity的唯一性了,因此需要一个回退字段标识唯一性.
     */
    internal val idn: Token get() = token ?: finalId

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
     * 根视图
     */
    var rootContentView: ActivityContentEntity = ActivityContentEntity()

    var composeWindow: ComposeWindow? = null

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

    open fun setContent(content: ApplicationComposableContent) {
        if (intent?.multiApplication == true) {
            mainCoroutineScope.launch {
                application(content = content)
            }
        } else {
            rootContentView.rootContent = content
            windowManager().attachWindow(rootContentView)
        }
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
     * 让生命周期进入onDestroy的方式有两种:
     * 1. 外部触发,手动点击窗口的"X"关闭按钮,compose window自动进入onDispose流程，生命周期走到ON_DESTROY状态，
     * 由于activity同步compose window的生命周期，于是activity也会进入[ON_DESTROY]状态，并调用[onDestroy]方法,移除WindowManager中注册的compose视图.
     *
     * 流程:
     * activity -> observe and sync -> window lifecycle
     * 即
     * close application window -> window lifecycle update to ON_DESTROY
     * -> activity sync window lifecycle -> activity lifecycle will set to ON_DESTROY and invoke onDestroy function
     *
     * 2. 内部触发, 手动调用finish方法
     *  2.1 如果compose window已显示,则从window manager中移除compose视图(此视图函数中调用了Window函数),application scope重组,
     *  不再调用此compose视图函数,Window函数就会从compose重组树上卸载,也就是窗口被移除,之后流程与1.相同
     *  2.2 如果compose window未显示,则直接使生命周期进入ON_DESTROY
     */
    @CallSuper
    open fun onDestroy() {
        /*
         * 关于窗口被手动或内部关闭, activity同步ON_DESTROY生命周期后会调用windowManager().unregister(idn),
         * 此方法会造成compose window被销毁,生命周期走到ON_DESTROY, activity又会同步生命周期,回调onDestroy,
         * 是否会造成循环调用的解释:
         *
         * 注：
         *
         * 移除窗口: 从WindowManager中移除ActivityContentEntity,
         * 此时application scope重组,程序窗口(compose window)消失,compose window的生命周期走到onDestroy
         *
         * 手动关闭程序窗口: 使用鼠标在窗口的右上角点击 X 按钮
         *
         * 窗口关闭\生命周期变化的两个流程：
         * 1. 如果外部触发,即手动关闭程序窗口,此时窗口生命周期走到ON_DESTROY,activity会同步窗口状态并移除生命周期监听,回调onDestroy方法移除窗口,
         * 移除窗口时窗口的生命周期已经destroy,不会再次触发onDestroy事件,且即使触发,由于activity已经移除生命周期监听,不会再次触发生命周期同步、回调onDestroy。
         *
         * 2. 如果是从程序中调用finish结束窗口,则会先移除窗口,窗口被移除会触发compose window的ON_DESTROY事件,activity会同步此状态并移除生命周期监听,回调onDestroy方法移除窗口,
         * 由于前面已经移除窗口,此时再次移除窗口是无效操作,不会再次触发ON_DESTROY事件,且activity已经移除生命周期监听,不会再次触发生命周期同步.
         *
         */
        windowManager().deAttachWindow(rootContentView)
        composeWindow = null
        savedState?.let { onSaveInstanceState(it) }
        finished = true
        activityManager().remove(idn)
    }

    /**
     * 手动结束activity,移除window
     */
    @CallSuper
    open fun finish() {
        if (rootContentView.isAttachedToApplication) {
            windowManager().deAttachWindow(rootContentView)
        } else {
            syncLife(ON_DESTROY)
            onDestroy()
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

    /**
     * 同步Compose Window的生命周期
     */
    @Composable
    open fun FrameWindowScope.Link2ComposeWindow(content: @Composable FrameWindowScope.() -> Unit) {
        //这里的lifecycle是composeContainer的提供的
        val lc: LifecycleOwner = LocalLifecycleOwner.current
        remember {
            lc.lifecycle.addObserver(parentLifecycleObserver)
        }
        this@Activity.composeWindow = this.window
        content()
    }

    companion object {
        const val RESULT_FLOW = "activity_result_flow"
        const val SUCCESS = 1
        const val FAILED = 0
    }
}
