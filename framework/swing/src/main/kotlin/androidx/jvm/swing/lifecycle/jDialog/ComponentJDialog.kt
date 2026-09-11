package androidx.jvm.swing.lifecycle.jDialog

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.viewmodel.MySavedStateViewModelFactory
import androidx.jvm.swing.lifecycle.jFrame.WindowAdapter2
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.github.knightwood.slf4j.kotlin.logFor
import java.awt.Dialog
import java.awt.Frame
import java.awt.GraphicsConfiguration
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener
import java.awt.event.WindowStateListener
import javax.swing.JDialog

/**
 * 带生命周期 / SavedState / ViewModel 支持的 [JDialog]。
 *
 * 生命周期来源与 [androidx.jvm.swing.lifecycle.jFrame.LifecycleJFrame] 一致：
 * - [WindowListener.windowOpened] → [onCreate]
 * - [WindowStateListener.windowStateChanged] → START/STOP/RESUME/PAUSE/DESTROY
 * - [WindowFocusListener] → RESUME/PAUSE（与 Activity 保持一致）
 *
 * 注意：Dialog 不是顶层窗口，通常由宿主 Activity 管理其显示/隐藏，
 * 因此其生命周期与宿主窗口事件一一对应即可，无需再做额外包装。
 */
open class ComponentJDialog : JDialog,
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner
{
    constructor() : super()
    constructor(owner: Frame?) : super(owner)
    constructor(owner: Frame?, modal: Boolean) : super(owner, modal)
    constructor(owner: Frame?, title: String?) : super(owner, title)
    constructor(owner: Frame?, title: String?, modal: Boolean) : super(owner, title, modal)
    constructor(owner: Frame?, title: String?, modal: Boolean, gc: GraphicsConfiguration?) : super(
        owner, title, modal, gc
    )

    constructor(owner: Dialog?) : super(owner)
    constructor(owner: Dialog?, modal: Boolean) : super(owner, modal)
    constructor(owner: Dialog?, title: String?) : super(owner, title)
    constructor(owner: Dialog?, title: String?, modal: Boolean) : super(owner, title, modal)
    constructor(owner: Dialog?, title: String?, modal: Boolean, gc: GraphicsConfiguration?) : super(
        owner, title, modal, gc
    )

    constructor(owner: Window?) : super(owner)
    constructor(owner: Window?, modalityType: ModalityType?) : super(owner, modalityType)
    constructor(owner: Window?, title: String?) : super(owner, title)
    constructor(owner: Window?, title: String?, modalityType: ModalityType?) : super(owner, title, modalityType)
    constructor(owner: Window?, title: String?, modalityType: ModalityType?, gc: GraphicsConfiguration?) : super(
        owner, title, modalityType, gc
    )

    private val logger = logFor("SwingDialog")

    /** 需要在显示之前赋值。 */
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

    // ---------------------------------------------------------------
    // ViewModelStoreOwner
    // ---------------------------------------------------------------
    private var _viewModelStore: ViewModelStore? = null
    override val viewModelStore: ViewModelStore
        get() {
            ensureViewModelStore()
            return _viewModelStore!!
        }

    // ---------------------------------------------------------------
    // SavedStateRegistryOwner
    // ---------------------------------------------------------------
    @Suppress("LeakingThis")
    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ---------------------------------------------------------------
    // HasDefaultViewModelProviderFactory
    // ---------------------------------------------------------------
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        MySavedStateViewModelFactory
    }

    override val defaultViewModelCreationExtras: CreationExtras
        get() {
            val extras = MutableCreationExtras()
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
            extras[VIEW_MODEL_STORE_OWNER_KEY] = this
            return extras
        }

    // ---------------------------------------------------------------
    // 生命周期事件桥接
    // ---------------------------------------------------------------
    @Transient
    private val innerWindowAdapter = object : WindowAdapter2() {
        override fun windowOpened(e: WindowEvent) {
            onCreate(savedState)
            super.windowOpened(e)
        }

        override fun windowClosing(e: WindowEvent) {
            finish()
            super.windowClosing(e)
        }

        override fun windowStateChanged(e: WindowEvent) {
            try {
                val event = when (e.newState) {
                    WindowEvent.WINDOW_ICONIFIED,
                    WindowEvent.WINDOW_DEACTIVATED -> Lifecycle.Event.ON_STOP

                    WindowEvent.WINDOW_DEICONIFIED,
                    WindowEvent.WINDOW_ACTIVATED -> Lifecycle.Event.ON_START

                    WindowEvent.WINDOW_LOST_FOCUS -> Lifecycle.Event.ON_PAUSE
                    WindowEvent.WINDOW_GAINED_FOCUS -> Lifecycle.Event.ON_RESUME
                    WindowEvent.WINDOW_CLOSED -> Lifecycle.Event.ON_DESTROY
                    else -> throw IllegalStateException("Unknown window state: ${e.newState}")
                }
                syncLife(event)
                when (event) {
                    Lifecycle.Event.ON_START -> onStart()
                    Lifecycle.Event.ON_RESUME -> onResume()
                    Lifecycle.Event.ON_PAUSE -> onPause()
                    Lifecycle.Event.ON_STOP -> onStop()
                    Lifecycle.Event.ON_DESTROY -> onDestroy()
                    else -> {}
                }
            } catch (ex: Exception) {
                logger.error("同步 Dialog 生命周期失败", ex)
            }
            super.windowStateChanged(e)
        }
    }

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED

        addWindowStateListener(innerWindowAdapter)
        addWindowListener(innerWindowAdapter)
        addWindowFocusListener(innerWindowAdapter)

        // 默认不直接关闭，走 finish() 自定义流程
        defaultCloseOperation = DO_NOTHING_ON_CLOSE

        // DESTROY 时清理 ViewModelStore
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    viewModelStore.clear()
                }
            }
        })

        savedStateRegistryController.performAttach()
        enableSavedStateHandles()
    }

    private fun syncLife(event: Lifecycle.Event) {
        lifecycleRegistry.currentState = event.targetState
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    // ---------------------------------------------------------------
    // 覆写监听器添加方法，统一路由到 innerWindowAdapter
    // ---------------------------------------------------------------
    override fun addWindowStateListener(listener: WindowStateListener) {
        innerWindowAdapter.windowStateListener = listener
    }

    override fun removeWindowStateListener(listener: WindowStateListener) {
        innerWindowAdapter.windowStateListener = null
    }

    override fun addWindowFocusListener(listener: WindowFocusListener) {
        innerWindowAdapter.windowFocusListener = listener
    }

    override fun removeWindowFocusListener(listener: WindowFocusListener) {
        innerWindowAdapter.windowFocusListener = null
    }

    override fun addWindowListener(listener: WindowListener) {
        innerWindowAdapter.windowListener = listener
    }

    override fun removeWindowListener(listener: WindowListener) {
        innerWindowAdapter.windowListener = null
    }

    // ---------------------------------------------------------------
    // 生命周期回调
    // ---------------------------------------------------------------
    @CallSuper
    open fun onCreate(savedInstanceState: SavedState?) {
        savedStateRegistryController.performRestore(savedInstanceState)
        syncLife(Lifecycle.Event.ON_CREATE)
    }

    @CallSuper
    open fun onSaveInstanceState(outState: SavedState) {
        savedStateRegistryController.performSave(outState)
    }

    @CallSuper
    open fun onStart() {}

    @CallSuper
    open fun onResume() {}

    @CallSuper
    open fun onPause() {}

    @CallSuper
    open fun onStop() {}

    @CallSuper
    open fun onDestroy() {
        savedState?.let { onSaveInstanceState(it) }
    }

    open fun finish() {
        isVisible = false
        dispose()
    }

    private fun ensureViewModelStore() {
        if (_viewModelStore == null) {
            _viewModelStore = ViewModelStore()
        }
    }
}
