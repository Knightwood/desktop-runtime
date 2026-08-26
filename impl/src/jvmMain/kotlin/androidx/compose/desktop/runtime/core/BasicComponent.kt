package androidx.compose.desktop.runtime.core

import androidx.compose.desktop.runtime.viewmodel.createVM
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
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
import kotlin.reflect.KClass

/**
 * 无ui的通用生命周期组件,提供如下功能:
 * 1. 生命周期\同步父组件生命周期,生命周期事件回调
 * 2. ViewModelStoreOwner
 * 3. SavedStateRegistryOwner\自动保存恢复状态
 *
 * ```
 * open class ScreenComponent() : IComponent() {
 *
 * }
 *
 * val screen = ScreenComponent()
 * screen.attach(Token("component_1"),lifecycleOwner.lifecycle)
 * ```
 */
abstract class BasicComponent : LifecycleComponent(),
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner,
    InstanceKoinComponent {

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
        SavedStateRegistryController.Companion.create(this)

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

    override fun onCreate(savedInstanceState: SavedState?) {
        savedStateRegistryController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: SavedState) {
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
//        logger.info("保存状态，uuid:$uuid")
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
}
