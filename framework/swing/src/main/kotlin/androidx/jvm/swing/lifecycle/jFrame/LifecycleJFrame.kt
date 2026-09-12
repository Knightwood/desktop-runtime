package androidx.jvm.swing.lifecycle.jFrame

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.core.getServiceInstance
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
import androidx.compose.desktop.runtime.savestate.Token
import androidx.compose.desktop.runtime.utils.WeakReferenceDelegate
import androidx.core.bundle.Bundle
import androidx.jvm.swing.lifecycle.core.intent.LaunchJFrameIntent
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.jvm.system.di.inject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import com.github.knightwood.slf4j.kotlin.logFor
import java.awt.GraphicsConfiguration
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener
import java.awt.event.WindowStateListener
import javax.swing.JFrame

/**
 *
 * 生成并显示一个JFrame有两种方式
 *
 * 方式1:
 * ```
 * val jFrame = BookEditorExample()
 * jFrame.isVisible = true
 * ```
 *
 * 方式2:
 * ```
 * LaunchJFrameIntent intent = new LaunchJFrameIntent(this,BookEditorExample.class, LaunchMode.STANDARD);
 * JFrameManager.openJFrame(intent);
 * ```
 *
 * WindowStateListener与 WindowListener
 * 事件分发源码:
```
 * protected void processEvent(AWTEvent e) {
 *         if (e instanceof WindowEvent) {
 *             switch (e.getID()) {
 *                 case WindowEvent.WINDOW_OPENED:
 *                 case WindowEvent.WINDOW_CLOSING:
 *                 case WindowEvent.WINDOW_CLOSED:
 *                 case WindowEvent.WINDOW_ICONIFIED:
 *                 case WindowEvent.WINDOW_DEICONIFIED:
 *                 case WindowEvent.WINDOW_ACTIVATED:
 *                 case WindowEvent.WINDOW_DEACTIVATED:
 *                     processWindowEvent((WindowEvent)e); //让WindowStateListener处理
 *                     break;
 *                 case WindowEvent.WINDOW_GAINED_FOCUS:
 *                 case WindowEvent.WINDOW_LOST_FOCUS:
 *                     processWindowFocusEvent((WindowEvent)e);
 *                     break;
 *                 case WindowEvent.WINDOW_STATE_CHANGED:
 *                     processWindowStateEvent((WindowEvent)e); //让WindowStateListener处理
 *                     break;
 *             }
 *             return;
 *         }
 *         super.processEvent(e);
 *     }
 * ```
 * 可以看到 WindowListener和WindowStateListener不会同时触发,前者处理了各种状态事件,后者只处理WINDOW_STATE_CHANGED事件.
 * 一句话概括: 事件总会区分发生前和完成后两个时机,WindowListener会在事件发生前回调,WindowStateListener则在事件完成后回调,告诉你事件已经完成.
 *
 * 以WINDOW_ICONIFIED为例:
 * 当你点击最小化按钮时，事件源头会先后产生两个不同 ID 的事件, AWT 事件队列里会依次放入两个事件：
 * WINDOW_ICONIFIED（ID = 401） //最小化事件,此时窗口还没有缩小
 * WINDOW_STATE_CHANGED（ID = 406） //状态以改变事件,此时窗口以完成最小化. 里面存储着新旧状态,其中新状态在这里是WINDOW_ICONIFIED.
 * 它们不是“二选一”，而是先发生后发生的关系, 点击按钮先产生了WINDOW_ICONIFIED表示窗口即将最小化,触发WindowListener回调,
 * 窗口最小化完成后又产生了WINDOW_STATE_CHANGED表示刚才的事件(窗口最小化)已完成,触发WindowStateListener回调.
 *
 * 因为关注点不同，需要不同的处理时机：
 *
 * |事件	|触发时机|你能做的事|
 * |--|--|--|
 * |WINDOW_ICONIFIED	|状态即将改变（最小化动画开始前）|	暂停动画、释放显存、保存UI状态（此时窗口还没缩小）|
 * | WINDOW_STATE_CHANGED	|状态已经改变（最小化完成后）|	查询最终状态值（getNewState()）、调整布局（窗口尺寸已变）|
 *
 * 如果你只监听 ICONIFIED(也就是在事件发生前), 在回调里调用 frame.getWidth() 可能拿到的是缩小前的宽高；而 STATE_CHANGED 触发时，宽高已经是缩小后的值了。
 *
 *
 * 也就是
 * WindowListener 只处理“动作语义”的事件（打开、关闭、最小化、激活等）
 * WindowStateListener 只处理“状态数值变化”的事件（状态值变了）
 *
 *
 */
open class LifecycleJFrame : JFrame, LifecycleOwner,
    InstanceKoinComponent {
    constructor() : super()
    constructor(gc: GraphicsConfiguration?) : super(gc)
    constructor(title: String?) : super(title)
    constructor(title: String?, gc: GraphicsConfiguration?) : super(title, gc)

    private val logger = logFor("SwingWindow")
    val stateSaver by inject<ApplicationSaveStateSaver>()
    var intent by WeakReferenceDelegate<LaunchJFrameIntent>()

    /**
     * 是否注册到了JFrameManager
     */
    private var registered = false

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
     * 此变量用于存放所有需要保存的状态.
     * 状态来自[onSaveInstanceState]、[SavedStateRegistry]等
     *
     * 手动赋值后此变量的get方法将返回手动赋予的值.
     * 如果未手动赋值且token存在,此变量的get方法将自动在[ApplicationSaveStateSaver]中使用token注册一个SaveState.
     */
    var savedState: SavedState? = null
        set(value) {
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                throw IllegalStateException("Cannot set SavedState after CREATED")
            }
            field = value
        }
        get() {
            if (field == null) {
                val id = token ?: return null
                return stateSaver.obtain(id)
            } else {
                return field
            }
        }

    @Suppress("LeakingThis")
    protected var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    @Transient
    private val innerWindowAdapter = object : WindowAdapter2() {

        override fun windowIconified(e: WindowEvent) {
            syncLifecycleByState(WindowEvent.WINDOW_ICONIFIED)
            super.windowIconified(e)
        }

        override fun windowDeiconified(e: WindowEvent) {
            syncLifecycleByState(WindowEvent.WINDOW_DEICONIFIED)
            super.windowDeiconified(e)
        }

        override fun windowActivated(e: WindowEvent) {
            syncLifecycleByState(WindowEvent.WINDOW_ACTIVATED)
            super.windowActivated(e)
        }

        override fun windowLostFocus(e: WindowEvent) {
            syncLifecycleByState(WindowEvent.WINDOW_LOST_FOCUS)
            super.windowLostFocus(e)
        }

        override fun windowGainedFocus(e: WindowEvent) {
            syncLifecycleByState(WindowEvent.WINDOW_GAINED_FOCUS)
            super.windowGainedFocus(e)
        }

        override fun windowClosing(e: WindowEvent) {
            finish()
            super.windowClosing(e)
        }
    }

    init {
        super.addWindowStateListener(innerWindowAdapter)
        //默认关闭窗口行为指定为什么都不做，然后监听窗口关闭操作，在窗口关闭时使用自定义的关闭流程
//        defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        /*
         * 监听窗口事件
         */
        super.addWindowListener(innerWindowAdapter)
        super.addWindowFocusListener(innerWindowAdapter)
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    override fun setVisible(b: Boolean) {
        super.setVisible(b)
        if (b) {
            // onCreate方法要尽可能发生的更早, 我决定让他在窗口打开前回调
            // 打开窗口时 先是windowActivated-> windowGainedFocus -> windowOpened
            // 前两个事件会多次调用，windowOpened会调用一次但时机太晚，不能在windowOpened中调用
            onCreate(savedState)
        }
    }

    /**
     * 此方法可选调用,
     * 如果希望用JFrameManager管理JFrame, 使用Intent
     */
    fun attach(intent: LaunchJFrameIntent) {
        this.intent = intent
        getServiceInstance<JFrameManager>().register(idn, this)
        registered = true
    }

    //<editor-fold desc="result callback">
    internal val internalResultFlow
        get() = intent?.getMailBox<JFrameResult>(LifecycleJFrame.DEFAULT_RESULT_FLOW)
            ?: throw IllegalStateException("jframe_result_flow can't be null")

    /**
     * @param resultCode 结果码，[LifecycleJFrame.SUCCESS]表示成功，[LifecycleJFrame.FAILED]表示失败
     */
    open fun setResult(resultCode: Int, data: Bundle? = null) {
        internalResultFlow.tryEmit(JFrameResult(resultCode, data))
    }

    //</editor-fold>
    /**
     * 将窗口事件翻译成对应的生命周期事件
     */
    private fun syncLifecycleByState(state: Int) {
        try {
            val lifecycleEvent = when (state) {
                WindowEvent.WINDOW_OPENED -> Lifecycle.Event.ON_CREATE

                WindowEvent.WINDOW_ICONIFIED,
                WindowEvent.WINDOW_DEACTIVATED,
                    -> Lifecycle.Event.ON_STOP

                WindowEvent.WINDOW_DEICONIFIED,
                WindowEvent.WINDOW_ACTIVATED,
                    -> Lifecycle.Event.ON_START

                WindowEvent.WINDOW_LOST_FOCUS -> Lifecycle.Event.ON_PAUSE
                WindowEvent.WINDOW_GAINED_FOCUS -> Lifecycle.Event.ON_RESUME

                WindowEvent.WINDOW_CLOSING,
                WindowEvent.WINDOW_CLOSED,
                    -> Lifecycle.Event.ON_DESTROY

                else -> {
                    Lifecycle.Event.ON_ANY
                }
            }
            logger.debug("syncLife life:{}", lifecycleEvent)
            syncLife(lifecycleEvent)
            when (lifecycleEvent) {
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_PAUSE -> onPause()
                Lifecycle.Event.ON_STOP -> onStop()
                Lifecycle.Event.ON_DESTROY -> onDestroy()
                Lifecycle.Event.ON_START -> onStart()
                // on_create事件不需要同步
                else -> {}
            }
        } catch (e: Exception) {
            logger.error("同步生命周期失败", e)
        }
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

    //<editor-fold desc="重写父级方法">
    override fun addWindowStateListener(listener: WindowStateListener) {
        this.innerWindowAdapter.windowStateListener = listener
    }

    override fun removeWindowStateListener(listener: WindowStateListener) {
        this.innerWindowAdapter.windowStateListener = null
    }

    override fun addWindowFocusListener(listener: WindowFocusListener) {
        this.innerWindowAdapter.windowFocusListener = listener
    }

    override fun removeWindowFocusListener(listener: WindowFocusListener) {
        this.innerWindowAdapter.windowFocusListener = null
    }

    override fun addWindowListener(listener: WindowListener) {
        this.innerWindowAdapter.windowListener = listener
    }

    override fun removeWindowListener(listener: WindowListener) {
        this.innerWindowAdapter.windowListener = null
    }
    //</editor-fold>

    @CallSuper
    open fun onCreate(savedInstanceState: SavedState?) {
        this.syncLife(Lifecycle.Event.ON_CREATE)
    }

    @CallSuper
    open fun onSaveInstanceState(outState: SavedState) {

    }

    open fun onReStart(intent: LaunchJFrameIntent) {}
    open fun onStart() {}

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
        savedState?.let { onSaveInstanceState(it) } ?: logger.warn("no savedState, not save data")
        if (registered) {
            getServiceInstance<JFrameManager>().remove(idn)
        }
    }

    open fun finish() {
        this.isVisible = false
        this.dispose()
    }

    companion object {
        const val DEFAULT_RESULT_FLOW = "jframe_result_flow"
        const val SUCCESS = 1
        const val FAILED = 0
    }
}


