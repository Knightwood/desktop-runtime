package androidx.compose.desktop.runtime.activity

import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.desktop.runtime.context.IContext
import androidx.compose.desktop.runtime.context.ThemedContext
import androidx.compose.desktop.runtime.window.DesktopWindow
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import java.util.UUID

/**
 * 启动模式，默认为标准模式，即多个实例可以同时存在。
 */
enum class LaunchMode {
    SINGLE_INSTANCE,
    STANDARD,
}

fun interface ActivityResult {
    fun invoke(resultCode: Int, data: Any?)
}

/**
 * 如下是jb对于window的生命周期描述。
 * | Swing listener callbacks | Lifecycle event | Lifecycle state change |
 * |--------------------------|-----------------|------------------------|
 * | windowIconified(最小化)     | ON_STOP         | STARTED → CREATED      |
 * | windowDeiconified(还原)    | ON_START        | CREATED → STARTED      |
 * | windowLostFocus(失去焦点)    | ON_PAUSE        | RESUMED → STARTED      |
 * | windowGainedFocus(获得焦点)  | ON_RESUME       | STARTED → RESUMED      |
 * | dispose(移除)              | ON_DESTROY      | CREATED → DESTROYED    |
 *
 * activity的生命周期并不完全与window同步.
 * window被移除时，会回调[Activity.onDestroyView]，activity不会进入onDestroy状态。
 * 只有在调用[Activity.finish]，activity的生命周期才会进行到onDestroy。
 */
abstract class Activity : ThemedContext(), LifecycleOwner, LifecycleEventObserver {

    lateinit var mWindow: DesktopWindow
    lateinit var intent: Intent

    @Suppress("LeakingThis")
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this@Activity)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    private val uuid = UUID.randomUUID()
    private var result: ActivityResult? = null

    /**
     * activity实现了IContext接口
     */
    val context get() = this

    /**
     * 1. activity将自己注册进[ActivityManager]
     * 2. 开始自己的生命周期
     * 3. 生成mWindow，并调用[onCreate]方法
     */
    internal fun attach(
        context: IContext,
        intent: Intent,
        block: ActivityResult?
    ) {
        mBase = context
        this.intent = intent
        this.result = block
        if (!this::mWindow.isInitialized) {
            mWindow = DesktopWindow(this, windowManager(), intent.multiApplication)
        }
        ActivityManager.register(uuid, this@Activity)
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
        onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    /**
     * [ComposeView]中观察了Window的生命周期，并同步给activity的[lifecycleRegistry]
     * 但是，不能同步[ON_DESTROY]状态，因为activity的生命周期理应比window更长。
     *
     * @param event 需要同步的生命周期事件
     * @param destroy 是否同步[ON_DESTROY]，在同步window生命周期是要求此参数为false。
     */
    private fun syncLife(event: Lifecycle.Event, destroy: Boolean = false) {
        if (event != ON_DESTROY || destroy) {
            lifecycleRegistry.currentState = event.targetState
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }

    /**
     * 观察window的生命周期，并进行部分同步
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        syncLife(event)
        when (event) {
            ON_RESUME -> onResume()
            ON_PAUSE -> onPause()
            ON_STOP -> onStop()
            ON_DESTROY -> onDestroyView()//window不显示，生命周期走到了destroy，但是activity还不能到destroy的时候
            ON_START -> onStart()
            else -> {}
        }
    }

    /**
     * Called when the activity is first created.
     *
     * @param data 启动此Activity附带的数据
     */
    @CallSuper
    open fun onCreate() {
    }

    open fun onStart() {}

    fun setContentView(content: @Composable ApplicationScope.() -> Unit) {
        if (!this::mWindow.isInitialized) {
            throw IllegalStateException("window is not initialized")
        }
        if (this.mWindow.content != null) {
            throw IllegalStateException("window content is not null, setContentView can only call once time")
        }
        mWindow(content)
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

    @CallSuper
    open fun onDestroyView() {
    }

    @CallSuper
    open fun onDestroy() {
    }

    @CallSuper
    open fun finish() {
        syncLife(Lifecycle.Event.ON_DESTROY, true)
        onDestroy()
        if (!intent.multiApplication) { // 单application模式，关闭窗口
            if (intent.exitAppWhenEmpty) { //如果window栈为空时退出app为true
                exitApp()
            } else {
                windowManager().deAttachWindow(mWindow)
            }
        }
        ActivityManager.remove(uuid)
        mWindow.release()//如果是多实例模式，进行到这一步时，应用的生命就结束了
    }

    /**
     * [Intent.multiApplication]为true，不支持[hide]/[show]
     */
    @CallSuper
    open fun hide() {
        windowManager().deAttachWindow(mWindow)
    }

    /**
     * [Intent.multiApplication]为true，不支持[hide]/[show]
     */
    @CallSuper
    open fun show() {
        synchronized(this) {//避免重复添加显示界面
            if (mWindow.isDeAttached)
                windowManager().attachWindow(mWindow)
        }
    }

    /**
     * 设置返回数据
     *
     * @param resultCode 结果码，[SUCCESS]表示成功，[FAILED]表示失败
     */
    open fun setResult(resultCode: Int, data: Any? = null) {
        result?.invoke(resultCode, data)
    }

    /**
     * 创建一个ComposeView，并绑定生命周期。
     *
     * @param state 窗口状态，默认为[rememberWindowState]
     * @param onCloseRequest 关闭窗口的回调，默认为[finish]关闭当前页面，也可以修改调用[exitApp]退出应用
     * @param visible 窗口是否可见，默认为true
     * @param title 窗口标题，默认为"Untitled"
     * @param icon 窗口图标，默认为null
     * @param undecorated 是否无边框，默认为false
     * @param transparent 是否透明，默认为false
     * @param resizable 是否可resize，默认为true
     * @param enabled 是否启用，默认为true
     * @param focusable 是否可聚焦，默认为true
     * @param alwaysOnTop 是否一直置顶，默认为false
     * @param onPreviewKeyEvent 预处理按键事件，默认为{@code false}
     * @param onKeyEvent 处理按键事件，默认为{@code false}
     * @param content 窗口内容
     */
    @Composable
    fun ComposeView(
        state: WindowState = rememberWindowState(),
        onCloseRequest: () -> Unit = {
            hide()
        },
        visible: Boolean = true,
        title: String = "Untitled",
        icon: Painter? = null,
        undecorated: Boolean = false,
        transparent: Boolean = false,
        resizable: Boolean = true,
        enabled: Boolean = true,
        focusable: Boolean = true,
        alwaysOnTop: Boolean = false,
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
        content: @Composable FrameWindowScope.() -> Unit
    ) {
        Window(
            onCloseRequest = onCloseRequest,
            state = state,
            visible = visible,
            title = title,
            icon = icon,
            undecorated = undecorated,
            transparent = transparent,
            resizable = resizable,
            enabled = enabled,
            focusable = focusable,
            alwaysOnTop = alwaysOnTop,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
            content = {
                val lc: LifecycleOwner = LocalLifecycleOwner.current
                remember {
                    lc.lifecycle.addObserver(this@Activity)
                }
                content()
            }
        )
    }

    companion object {
        const val SUCCESS = 1
        const val FAILED = 0
    }
}
