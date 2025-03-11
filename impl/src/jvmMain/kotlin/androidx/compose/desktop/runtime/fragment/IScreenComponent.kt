package androidx.compose.desktop.runtime.fragment

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.activity.IBundleHolder
import androidx.compose.desktop.runtime.viewmodel.createVM
import androidx.core.bundle.Bundle
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import java.util.*
import kotlin.reflect.KClass

interface ScreenComponentCallback {
    /**
     * 子组件复写此方法可以知道当前生命周期
     */
    fun onStateChanged(event: Lifecycle.Event)
}

/**
 * 提供了生命周期、ViewModelStoreOwner、SavedStateRegistryOwner等基础组件
 * 可用于作为一个ui片段的容器，类似于fragment
 *
 * ```
 * open class ScreenComponent() : IScreenComponent() {
 *
 * }
 *
 * val screen =ScreenComponent()
 * screen.prepare(lifecycleOwner.lifecycle, componentBundle)
 * ```
 */
abstract class IScreenComponent() : ViewModelStoreOwner, LifecycleOwner, LifecycleEventObserver,
    HasDefaultViewModelProviderFactory, SavedStateRegistryOwner, ScreenComponentCallback {
    // Internal unique name for this fragment;
    var uuid: String = UUID.randomUUID().toString()
    lateinit var bundleHolder: IBundleHolder
    var clearBundle: Boolean = true

    @Suppress("LeakingThis")
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private var _viewModelStore: ViewModelStore? = null
    override val viewModelStore: ViewModelStore
        /**
         * Returns the [ViewModelStore] associated with this activity
         *
         * @return a [ViewModelStore]
         * @throws IllegalStateException if called before the Activity is attached
         *    to the Application instance i.e., before onCreate()
         */
        get() {
            ensureViewModelStore()
            return _viewModelStore!!
        }

    @Suppress("LeakingThis")
    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        @Suppress("LeakingThis")
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

    /**
     * 在生成实例后，调用此方法开始此类的生命周期流程
     */
    fun prepare(parentLifecycle: Lifecycle, bundleHolder: IBundleHolder) {
        this.bundleHolder = bundleHolder
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        onCreate(bundleHolder.obtainBundleNullable(uuid))
        parentLifecycle.addObserver(this)
    }

    /**
     * observe activity lifecycle
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_DESTROY -> {
                if (clearBundle) {
                    bundleHolder.clearBundle(uuid)
                } else {
                    onSaveInstanceState(bundleHolder.obtainBundle(uuid))
                }
            }

            else -> {}
        }
        syncLife(event)
        onStateChanged(event)
    }

    /**
     * sync activity lifecycle to fragment lifecycle
     */
    private fun syncLife(event: Lifecycle.Event) {
        lifecycleRegistry.currentState = event.targetState
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    @CallSuper
    open fun onCreate(savedInstanceState: Bundle?) {
//        logger.info("恢复状态，uuid:$uuid")
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    @CallSuper
    fun onSaveInstanceState(outState: Bundle) {
        savedStateRegistryController.performSave(outState)
//        logger.info("保存状态，uuid:$uuid")
    }

    private fun ensureViewModelStore() {
        if (_viewModelStore == null) {
            _viewModelStore = ViewModelStore()
        }
    }

    /**
     * 手动结束生命周期
     */
    fun release() {
        syncLife(ON_DESTROY)
        if (clearBundle) {
            bundleHolder.clearBundle(uuid)
        } else {
            onSaveInstanceState(bundleHolder.obtainBundle(uuid))
        }
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return createVM(modelClass.java, extras)
            }
        }
    }

    public override val defaultViewModelCreationExtras: CreationExtras
        get() {
            val extras = MutableCreationExtras()
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
            extras[VIEW_MODEL_STORE_OWNER_KEY] = this
            return extras
        }
}
