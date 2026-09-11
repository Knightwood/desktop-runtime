package androidx.jvm.swing.lifecycle.jPanel

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.viewmodel.MySavedStateViewModelFactory
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
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
import java.awt.LayoutManager
import javax.swing.JPanel
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

/**
 * 一个基于 [JPanel] 的 Fragment，同时支持：
 * - [Lifecycle]：与宿主 [LifecycleOwner]（通常是 [androidx.jvm.swing.lifecycle.jFrame.ComponentJFrame]）联动，
 *   宿主 DESTROY 时销毁自身；本身状态推进到 STARTED/RESUMED 时跟随宿主。
 * - [SavedStateRegistry]：可在 [onSaveInstanceState] 中持久化状态。
 * - [ViewModelStore]：通过 [defaultViewModelProviderFactory] 创建 ViewModel。
 *
 * 生命周期来源：
 * - 当被加入到一个 [LifecycleOwner]（祖先链中最近的）容器时，绑定其生命周期，
 *   并把自身的 state 推进到与宿主一致。
 * - 被移除时，触发 [ON_DESTROY]，清理 ViewModelStore。
 */
open class ComponentJPanel : JPanel,
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner
{
    constructor() : super()
    constructor(isDoubleBuffered: Boolean) : super(isDoubleBuffered)
    constructor(layout: LayoutManager?) : super(layout)
    constructor(layout: LayoutManager?, isDoubleBuffered: Boolean) : super(layout, isDoubleBuffered)

    private val logger = logFor("SwingFragment")

    @Suppress("LeakingThis")
    protected var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    /** 宿主（通常是 Activity），在 ancestorAdded 时确定，用于转发生命周期。 */
    private var hostOwner: LifecycleOwner? = null

    private var hostObserver: androidx.lifecycle.LifecycleEventObserver? = null

    /**
     * 需要在被加入容器之前赋值（对应 Activity 的构造后、onCreate 之前）。
     */
    var savedState: SavedState? = null
        set(value) {
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                throw IllegalStateException("Cannot set SavedState after CREATED")
            }
            field = value
        }

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

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED

        // 当 DESTROY 时清理 ViewModelStore
        lifecycle.addObserver(object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    viewModelStore.clear()
                }
            }
        })

        // attach / 恢复 saved state
        savedStateRegistryController.performAttach()
        enableSavedStateHandles()

        // 通过 AncestorListener 感知宿主
        addAncestorListener(object : AncestorListener {
            override fun ancestorAdded(event: AncestorEvent?) {
                val owner = findLifecycleOwner(event?.component?.parent)
                if (owner != null && owner !== hostOwner) {
                    bindHost(owner)
                }
            }

            override fun ancestorRemoved(event: AncestorEvent?) {
                // 从容器移除等价于销毁（JPanel 没有系统级"关闭"概念）
                unbindHost(destroy = true)
            }

            override fun ancestorMoved(event: AncestorEvent?) {
                // 位置变化不处理
            }
        })
    }

    /** 从 parent 链上找出最近的 LifecycleOwner。 */
    private fun findLifecycleOwner(start: java.awt.Container?): LifecycleOwner? {
        var c: java.awt.Container? = start
        while (c != null) {
            if (c is LifecycleOwner) return c
            c = c.parent
        }
        return null
    }

    /**
     * 绑定宿主生命周期：把宿主的 Lifecycle.Event 转发到自身，
     * 并把宿主当前已到达的 state 同步过来。
     */
    private fun bindHost(owner: LifecycleOwner) {
        hostOwner = owner
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            // 宿主 DESTROY 时，自身销毁
            if (event == Lifecycle.Event.ON_DESTROY) {
                unbindHost(destroy = true)
            } else {
                handleLifecycleEvent(event)
            }
        }
        hostObserver = observer
        owner.lifecycle.addObserver(observer)

        // 首次绑定时，把自身状态对齐到宿主当前状态
        val hostState = owner.lifecycle.currentState
        if (hostState.isAtLeast(Lifecycle.State.CREATED) &&
            !lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)
        ) {
            onCreate(savedState)
        }
        if (hostState.isAtLeast(Lifecycle.State.STARTED) &&
            lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED) &&
            !lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) {
            onStart()
        }
        if (hostState.isAtLeast(Lifecycle.State.RESUMED) &&
            lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED) &&
            !lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            onResume()
        }
    }

    /** 解绑宿主；destroy=true 时触发 ON_DESTROY。 */
    private fun unbindHost(destroy: Boolean) {
        hostObserver?.let { hostOwner?.lifecycle?.removeObserver(it) }
        hostObserver = null
        hostOwner = null
        if (destroy) {
            if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
                onDestroy()
            }
        }
    }

    private fun handleLifecycleEvent(event: Lifecycle.Event) {
        try {
            lifecycleRegistry.handleLifecycleEvent(event)
            when (event) {
                Lifecycle.Event.ON_CREATE -> onCreate(savedState)
                Lifecycle.Event.ON_START -> onStart()
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_PAUSE -> onPause()
                Lifecycle.Event.ON_STOP -> onStop()
                Lifecycle.Event.ON_DESTROY -> onDestroy()
                else -> {}
            }
        } catch (e: Exception) {
            logger.error("Fragment 生命周期同步失败", e)
        }
    }

    private fun ensureViewModelStore() {
        if (_viewModelStore == null) {
            _viewModelStore = ViewModelStore()
        }
    }

    // ---------------------------------------------------------------
    // 生命周期回调
    // ---------------------------------------------------------------
    @CallSuper
    open fun onCreate(savedInstanceState: SavedState?) {
        savedStateRegistryController.performRestore(savedInstanceState)
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
}
