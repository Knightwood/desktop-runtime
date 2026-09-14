package androidx.jvm.swing.lifecycle.core.intent

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener
import java.awt.event.WindowStateListener

/**
 * 窗口状态时序:
 * ```
 * 打开窗口：
 * 08:08:30.776 [AWT-EventQueue-0] INFO -- windowActivated
 * 08:08:30.778 [AWT-EventQueue-0] INFO -- windowGainedFocus
 * 08:08:30.779 [AWT-EventQueue-0] INFO -- windowOpened
 * 最小化：
 * 08:09:12.403 [AWT-EventQueue-0] INFO -- windowIconified
 * 08:09:12.405 [AWT-EventQueue-0] INFO -- windowLostFocus
 * 08:09:12.405 [AWT-EventQueue-0] INFO -- windowDeactivated
 * 还原：
 * 08:09:42.216 [AWT-EventQueue-0] INFO -- windowDeiconified
 * 08:09:42.249 [AWT-EventQueue-0] INFO -- windowActivated
 * 08:09:42.250 [AWT-EventQueue-0] INFO -- windowGainedFocus
 * 获取焦点：
 * 08:09:53.975 [AWT-EventQueue-0] INFO -- windowActivated
 * 08:09:54.007 [AWT-EventQueue-0] INFO -- windowGainedFocus
 * 失去焦点
 * 08:09:55.633 [AWT-EventQueue-0] INFO -- windowLostFocus
 * 08:09:55.633 [AWT-EventQueue-0] INFO -- windowDeactivated
 * 关闭窗口（HIDE_ON_CLOSE）：
 * 08:08:45.444 [AWT-EventQueue-0] INFO -- Closing
 * 08:08:45.451 [AWT-EventQueue-0] INFO -- windowLostFocus
 * 08:08:45.451 [AWT-EventQueue-0] INFO -- windowDeactivated
 * 关闭窗口（DISPOSE_ON_CLOSE）：
 * 08:13:18.964 [AWT-EventQueue-0] INFO -- Closing
 * 08:13:18.976 [AWT-EventQueue-0] INFO -- windowLostFocus
 * 08:13:18.976 [AWT-EventQueue-0] INFO -- windowDeactivated
 * 08:13:18.976 [AWT-EventQueue-0] INFO -- Closed
 * ```
 *
 *
 * 1. 此类用于将窗口状态转换成生命周期状态
 * 2. 将生命周期状态派发给lifecycleRegistry或手动处理
 * 3. 转发监听到的窗口状态
 *
 * 使用方式:
 *
 * ```
 *
 * private val lifecycleAdapter = WindowLifecycleAdapter(lifecycleRegistry)
 *
 * init {
 *     //初始化生命周期状态
 *     lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
 *     // 添加为窗口状态监听, 开始将窗口状态转换成生命周期状态,并派发给lifecycleRegistry
 *     super.addWindowListener(lifecycleAdapter)
 *     super.addWindowStateListener(lifecycleAdapter)
 *     super.addWindowFocusListener(lifecycleAdapter)
 * }
 * ```
 *
 * 注意:
 * addWindowListener、addWindowFocusListener等方法添加监听器会重置之前设置的监听器,
 * 因此, 在JFrame中使用时,
 * 将无法再次向窗口添加相关监听器,
 *
 */
class WindowLifecycleAdapter(
    /**
     * 手动处理窗口状态转换而来的生命周期状态
     */
    val lifecycleDispatcher: (Lifecycle.State) -> Unit,
) : WindowAdapter() {
    /**
     * 自动将生命周期状态派发给 lifecycleRegistry
     */
    constructor(lifecycleRegistry: LifecycleRegistry) :
            this(
                { state ->
                    lifecycleRegistry.currentState = state
                }
            )

    /*
    * 可选, 用于转发窗口状态, 解决 addWindowFocusListener、addWindowListener、addWindowStateListener
    * 等方法添加监听器会替换掉上一次添加的监听器, 导致窗口状态转生命周期状态功能被破坏
    */
    var windowListener: WindowListener? = null
    var windowStateListener: WindowStateListener? = null
    var windowFocusListener: WindowFocusListener? = null

    private val log: Logger = LoggerFactory.getLogger(WindowLifecycleAdapter::class.java)

    var isOpened = false
    var isMinimized = false
    var isDisposed = false
    var isFocused = false


    //<editor-fold desc="windowListener">

    override fun windowOpened(e: WindowEvent?) {
        windowListener?.windowOpened(e)
        isOpened = true
        updateLifecycle()
//        log.info("windowOpened")
    }

    override fun windowIconified(e: WindowEvent?) {
        windowListener?.windowIconified(e)
        isMinimized = true
        updateLifecycle()
//        log.info("windowIconified")
    }

    override fun windowDeiconified(e: WindowEvent?) {
        windowListener?.windowDeiconified(e)
        isMinimized = false
        updateLifecycle()
//        log.info("windowDeiconified")
    }

    override fun windowActivated(e: WindowEvent?) {
        windowListener?.windowActivated(e)
//        log.info("windowActivated")
    }

    override fun windowDeactivated(e: WindowEvent?) {
        windowListener?.windowDeactivated(e)
//        log.info("windowDeactivated")
    }

    override fun windowClosing(e: WindowEvent?) {
        windowListener?.windowClosing(e)
//        log.info("Closing")
    }

    override fun windowClosed(e: WindowEvent?) {
        windowListener?.windowClosed(e)
        isDisposed = true
        updateLifecycle()
//        log.info("Closed")
    }

    //</editor-fold>

    //<editor-fold desc="windowFocusListener">

    override fun windowGainedFocus(e: WindowEvent?) {
        windowFocusListener?.windowGainedFocus(e)
        isFocused = true
        updateLifecycle()
//        log.info("windowGainedFocus")
    }

    override fun windowLostFocus(e: WindowEvent?) {
        windowFocusListener?.windowLostFocus(e)
        isFocused = false
        updateLifecycle()
//        log.info("windowLostFocus")
    }
    //</editor-fold>

    //<editor-fold desc="windowStateListener">

    override fun windowStateChanged(e: WindowEvent?) {
        windowStateListener?.windowStateChanged(e)
    }

    //</editor-fold>
    /**
     * 复制自androidx.compose.ui.scene.ComposeContainer.updateLifecycleState
     */
    private fun updateLifecycle() {
        val state = when {
            isDisposed -> Lifecycle.State.DESTROYED
            !isOpened || isMinimized -> Lifecycle.State.CREATED
            isOpened && !isMinimized && isFocused -> Lifecycle.State.RESUMED
            else -> Lifecycle.State.STARTED
        }
        lifecycleDispatcher.invoke(state)
    }

}
