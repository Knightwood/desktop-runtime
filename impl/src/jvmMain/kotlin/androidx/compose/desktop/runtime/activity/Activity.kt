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
import androidx.core.bundle.Bundle
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
abstract class Activity : ThemedContext(), LifecycleOwner, LifecycleEventObserver {
    lateinit var mWindow: DesktopWindow
    lateinit var intent: Intent

    @Suppress("LeakingThis")
    protected var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this@Activity)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    /**
     * uuid是activity的唯一标识，可以用于activity保存和回复状态，从[ActivityManager]获取实例等
     */
    var uuid :String= UUID.randomUUID().toString()
        private set

    /**
     * 用于保存和恢复activity的状态
     */
    val bundle
        get() = activityManager().obtainBundle(uuid)
    private var result: ActivityResult? = null

    /**
     * activity实现了IContext接口
     */
    val context get() = this

    private var finished: Boolean = false

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
        intent.uuid?.let {
            this.uuid = it
        }
        this.result = block
        if (!this::mWindow.isInitialized) {
            mWindow = DesktopWindow(this, windowManager(), intent.multiApplication)
        }
        ActivityManager.register(uuid, this@Activity)
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        onCreate(activityManager().obtainBundleNullable(uuid))
    }

    /**
     * 观察Window的生命周期，并同步给activity的[lifecycleRegistry]
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
     * 当window销毁时，activity的生命周期就走到destroy
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
//        logger.info("window event: $event")
        syncLife(event, finished)
        when (event) {
            ON_RESUME -> onResume()
            ON_PAUSE -> onPause()
            ON_STOP -> onStop()
            ON_DESTROY -> {
                onSaveInstanceState(bundle)
                if (finished) {
                    onDestroy()
                }
            }

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
    open fun onCreate(savedInstanceState: Bundle?) {
        lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    open fun onSaveInstanceState(outState: Bundle) {
        val lifecycle = lifecycle
        if (lifecycle is LifecycleRegistry) {
            lifecycle.currentState = Lifecycle.State.CREATED
        }
    }

    open fun onStart() {}

   open fun setContent(content: @Composable ApplicationScope.() -> Unit) {
        if (!this::mWindow.isInitialized) {
            throw IllegalStateException("window is not initialized")
        }
        if (this.mWindow.contentShell != null) {
            throw IllegalStateException("window content is not null, setContentView can only call once time")
        }
        mWindow(content)

    }


//    fun test() {
//        setContent {
//            val b = 1
//            wrap {
//                println(b)
//            }
//        }
//    }
//
//    fun setContent(content: ContentWrapper.() -> Unit) {
//        val contentWrapper = object : ContentWrapper {
//            val a = 1
//            override fun wrap(content: @Composable () -> Unit): () -> Unit {
//                TODO("Not yet implemented")
//            }
//        }
//        contentWrapper.content()
//    }

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
    open fun onDestroy() {
    }

    /**
     * 1. 从ActivityManager中移出自己
     * 2. 从windowManager移出window，调用[onDestroy]方法
     * 3. 进入[ON_DESTROY]状态，并调用[onDestroy]方法
     */
    @CallSuper
    open fun finish() {
        finished = true
        if (intent.multiApplication) { // 多application模式，会结束整个应用进程
            ActivityManager.remove(uuid)
            mWindow.release()//多实例模式下，进行到这一步时，应用的生命就结束了
        } else {// 单application模式，关闭窗口
            ActivityManager.remove(uuid)
            mWindow.release()
        }
    }

    /**
     * [Intent.multiApplication]为true，不支持[hide]/[show]
     */
    @CallSuper
    open fun hide() {
        lifecycleScope.launch {
            mWindow.isHidden.value = true
        }
    }

    /**
     * [Intent.multiApplication]为true，不支持[hide]/[show]
     */
    open fun show() {
        lifecycleScope.launch {
            mWindow.isHidden.value = (false)
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
     * @param onCloseRequest 关闭窗口的回调，默认为[finish]关闭当前window，
     *    可以调用[hide]隐藏窗口，也可以调用[exitApp]结束应用进程。
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
        onCloseRequest: () -> Unit,
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
            visible = !mWindow.isHidden.value,
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
