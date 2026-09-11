package androidx.jvm.swing.lifecycle.jFrame

import androidx.annotation.CallSuper
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener
import java.awt.event.WindowStateListener

open class WindowAdapter2(
    var windowStateListener: WindowStateListener? = null,
    var windowListener: WindowListener? = null,
    var windowFocusListener: WindowFocusListener? = null,
) : WindowAdapter() {
    constructor(windowAdapter: WindowAdapter) : this(windowAdapter, windowAdapter, windowAdapter)

    @CallSuper
    override fun windowOpened(e: WindowEvent) {
        windowListener?.windowOpened(e)
    }

    @CallSuper
    override fun windowClosing(e: WindowEvent) {
        windowListener?.windowClosing(e)
    }

    @CallSuper
    override fun windowClosed(e: WindowEvent) {
        windowListener?.windowClosed(e)
    }

    @CallSuper
    override fun windowIconified(e: WindowEvent) {
        windowListener?.windowIconified(e)
    }

    @CallSuper
    override fun windowDeiconified(e: WindowEvent) {
        windowListener?.windowDeiconified(e)
    }

    @CallSuper
    override fun windowActivated(e: WindowEvent) {
        windowListener?.windowActivated(e)
    }

    @CallSuper
    override fun windowDeactivated(e: WindowEvent) {
        windowListener?.windowDeactivated(e)
    }

    @CallSuper
    override fun windowStateChanged(e: WindowEvent) {
        windowStateListener?.windowStateChanged(e)
    }

    @CallSuper
    override fun windowGainedFocus(e: WindowEvent) {
        windowFocusListener?.windowGainedFocus(e)
    }

    @CallSuper
    override fun windowLostFocus(e: WindowEvent) {
        windowFocusListener?.windowLostFocus(e)
    }
}
