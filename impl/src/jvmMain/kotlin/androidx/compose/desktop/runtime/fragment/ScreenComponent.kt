package androidx.compose.desktop.runtime.fragment

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.viewmodel.createVM
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.reflect.KClass

open class ScreenComponent(parent: LifecycleOwner? = null) : ViewModelStoreOwner, LifecycleOwner,
    HasDefaultViewModelProviderFactory, SavedStateRegistryOwner {

    @Suppress("LeakingThis")
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this@ScreenComponent)
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
            check(lifecycle.currentState == Lifecycle.State.INITIALIZED) {
                ("Your ScreenComponent is not yet attached to the " +
                        "parent. You can't request ViewModel before onCreate call.")
            }
            ensureViewModelStore()
            return _viewModelStore!!
        }

    @Suppress("LeakingThis")
    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
        onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
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
        savedStateRegistryController.performRestore(null)
    }

    private fun ensureViewModelStore() {
        if (_viewModelStore == null) {
            _viewModelStore = ViewModelStore()
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

    @CallSuper
    open fun onCreate() {
    }
}
