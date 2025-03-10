package androidx.compose.desktop.runtime.fragment

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.viewmodel.createVM
import androidx.compose.runtime.Composable
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

/**
 * ```
 * class TestComponent :Component()
 *
 * val a = TestComponent()
 * a.attach()
 * a.release()
 * ```
 */
abstract class Component() : ViewModelStoreOwner, LifecycleOwner, LifecycleEventObserver,
    HasDefaultViewModelProviderFactory, SavedStateRegistryOwner {

    private var finished: Boolean = false

    // Internal unique name for this fragment;
    var mWho: String = UUID.randomUUID().toString()

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
     * 如果构造函数中没有传入lifecycle。
     * 可以在生成实例后，调用此方法开始此类的生命周期流程
     */
    fun attach() {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        onCreate(provideSaveState())
    }

    /**
     * observe activity lifecycle
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_CREATE->{
                //add observer to attached activity
                attach()
            }
            ON_DESTROY -> {
                onSaveInstanceState(provideSaveState())
                if (finished) {
                    onDestroy()
                }
            }

            else -> {}
        }
        syncLife(event)
    }

    /**
     * 提供用于保存和回复状态的Bundle
     */
    abstract fun provideSaveState(): Bundle

    open fun onDestroy() {}

    /**
     * sync activity lifecycle to fragment lifecycle
     */
    private fun syncLife(event: Lifecycle.Event) {
        lifecycleRegistry.currentState = event.targetState
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    @CallSuper
    open fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    @CallSuper
    fun onSaveInstanceState(outState: Bundle) {
        savedStateRegistryController.performSave(outState)
    }

    private fun ensureViewModelStore() {
        if (_viewModelStore == null) {
            _viewModelStore = ViewModelStore()
        }
    }

    fun release() {
        syncLife(ON_DESTROY)
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
