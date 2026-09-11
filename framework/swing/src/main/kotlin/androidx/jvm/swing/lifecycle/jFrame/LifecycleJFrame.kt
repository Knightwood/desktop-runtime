package androidx.jvm.swing.lifecycle.jFrame

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedState
import com.github.knightwood.slf4j.kotlin.logFor
import java.awt.GraphicsConfiguration
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener
import java.awt.event.WindowStateListener
import javax.swing.JFrame

/**
 * ```
 * val activity = ExampleActivity()
 * activity.savedState = lastSaved
 * activity.isVisible = true
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
open class LifecycleJFrame : JFrame, LifecycleOwner {
    constructor() : super()
    constructor(gc: GraphicsConfiguration?) : super(gc)
    constructor(title: String?) : super(title)
    constructor(title: String?, gc: GraphicsConfiguration?) : super(title, gc)

    private val logger = logFor("SwingWindow")

    /**
     * 需要在生成实例后立即赋值
     */
    var savedState: SavedState? = null
        set(value) {
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                throw IllegalStateException("Cannot set SavedState after CREATED")
            }
            field = value
        }

    @Suppress("LeakingThis")
    protected var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    @Transient
    private val innerWindowAdapter = object : WindowAdapter2() {
        override fun windowOpened(e: WindowEvent) {
            // onCreate方法要尽可能发生的更早, 我决定让他在窗口打开前回调
            // 因此要在WindowListener中重写windowOpened方法,回调onCreate
            // 如果是在WindowStateListener中监听事件已完成再去回调onCreate, 则有些过晚了.
            // 其余的生命周期方法则可以在WindowStateListener监听到事件已完成后再去回调.
            onCreate(savedState)
            super.windowOpened(e)
        }

        override fun windowClosing(e: WindowEvent) {
            // 窗口关闭请求
            finish()
            super.windowClosing(e)
        }

        override fun windowStateChanged(e: WindowEvent) {
            try {
                val lifecycleEvent = when (e.newState) {
                    WindowEvent.WINDOW_ICONIFIED,
                    WindowEvent.WINDOW_DEACTIVATED,
                        -> Lifecycle.Event.ON_STOP

                    WindowEvent.WINDOW_DEICONIFIED,
                    WindowEvent.WINDOW_ACTIVATED,
                        -> Lifecycle.Event.ON_START

                    WindowEvent.WINDOW_LOST_FOCUS -> Lifecycle.Event.ON_PAUSE
                    WindowEvent.WINDOW_GAINED_FOCUS -> Lifecycle.Event.ON_RESUME
                    WindowEvent.WINDOW_CLOSED -> Lifecycle.Event.ON_DESTROY
                    else -> {
                        throw IllegalStateException()
                    }
                }
                syncLife(lifecycleEvent)
                when (lifecycleEvent) {
                    ON_RESUME -> onResume()
                    ON_PAUSE -> onPause()
                    ON_STOP -> onStop()
                    ON_DESTROY -> onDestroy()
                    ON_START -> onStart()
                    // on_create事件不需要同步
                    else -> {}
                }
            } catch (e: Exception) {
                logger.error("同步生命周期失败", e)
            }
            super.windowStateChanged(e)
        }
    }

    init {
        this.addWindowStateListener(innerWindowAdapter)
        //默认关闭窗口行为指定为什么都不做，然后监听窗口关闭操作，在窗口关闭时使用自定义的关闭流程
        defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        /*
         * 监听窗口事件
         */
        this.addWindowListener(innerWindowAdapter)
        this.addWindowFocusListener(innerWindowAdapter)
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
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
        this.syncLife(ON_CREATE)
    }

    @CallSuper
    open fun onSaveInstanceState(outState: SavedState) {

    }

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
        savedState?.let { onSaveInstanceState(it) }
    }

    open fun finish() {
        this.isVisible = false
        this.dispose()
    }
}


