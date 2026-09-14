package androidx.jvm.swing.lifecycle.jFrame

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.core.getServiceInstance
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
import androidx.compose.desktop.runtime.savestate.Token
import androidx.compose.desktop.runtime.utils.WeakReferenceDelegate
import androidx.core.bundle.Bundle
import androidx.jvm.swing.lifecycle.core.intent.LaunchJFrameIntent
import androidx.jvm.swing.lifecycle.core.intent.WindowLifecycleAdapter
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.jvm.system.di.inject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import com.github.knightwood.slf4j.kotlin.logFor
import java.awt.GraphicsConfiguration
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener
import java.awt.event.WindowStateListener
import javax.swing.JFrame

/**
 * https://pingfangx.github.io/java-tutorials/uiswing/events/windowlistener.html
 * 生成并显示一个JFrame有两种方式
 *
 * 方式1直接设置isVisible = true,即可显示窗口,
 * 此方式不具备获取启动参数、设置结果、自动获取SaveStated等
 * ```
 * val jFrame = BookEditorExample()
 * jFrame.isVisible = true
 * ```
 *
 * 方式2使用Intent启动, 被启动的JFrame将持有启动者提供的Intent作为沟通桥梁,
 * 使用Intent获取传递的启动参数, 通过Intent回传结果, 自动从ApplicationSaveStateSaver获取SaveStated等
 * ```
 * LaunchJFrameIntent intent = new LaunchJFrameIntent(this,BookEditorExample.class, LaunchMode.STANDARD);
 * JFrameManager.openJFrame(intent);
 * ```
 *
 * 注意:
 * JFrame默认关闭操作为HIDE_ON_CLOSE, 这意味着关闭窗口将不会触发OnDestroy事件
 * 需要将默认关闭操作设置为
 * setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 *
 *
 * ---
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

    private val lifecycleAdapter = WindowLifecycleAdapter(lifecycleRegistry)

    init {
        //监听生命周期事件并回调对应的生命周期方法
        lifecycleRegistry.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(
                source: LifecycleOwner,
                event: Lifecycle.Event,
            ) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> onCreate(savedState)
                    Lifecycle.Event.ON_START -> onStart()
                    Lifecycle.Event.ON_RESUME -> onResume()
                    Lifecycle.Event.ON_PAUSE -> onPause()
                    Lifecycle.Event.ON_STOP -> onStop()
                    Lifecycle.Event.ON_DESTROY -> onDestroy()
                    else -> Unit
                }
            }
        })
        //初始化生命周期
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        //添加窗口状态监听, 使WindowLifecycleAdapter将窗口状态转换成生命周期状态并派发给lifecycleRegistry
        //这里已重写相关方法, 需要使用super添加状态监听
        super.addWindowListener(lifecycleAdapter)
        super.addWindowFocusListener(lifecycleAdapter)
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


    //<editor-fold desc="重写父级方法">
    /*
    addWindowFocusListener、addWindowListener、addWindowStateListener
    等方法添加监听器会替换掉上一次添加的监听器, 由于我们已经将WindowLifecycleAdapter
    添加为相关的监听器, 如果再次向当前窗口添加监听器, 会破坏我们的窗口状态转生命周期状态功能,
    所以, 我们需要将相关的监听器添加到WindowLifecycleAdapter, 让WindowLifecycleAdapter转发窗口状态
    */

    override fun addWindowFocusListener(listener: WindowFocusListener) {
        this.lifecycleAdapter.windowFocusListener = listener
    }

    override fun removeWindowFocusListener(listener: WindowFocusListener) {
        this.lifecycleAdapter.windowFocusListener = null
    }

    override fun addWindowListener(listener: WindowListener) {
        this.lifecycleAdapter.windowListener = listener
    }

    override fun removeWindowListener(listener: WindowListener) {
        this.lifecycleAdapter.windowListener = null
    }
    //</editor-fold>

    @CallSuper
    open fun onCreate(savedInstanceState: SavedState?) {

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


