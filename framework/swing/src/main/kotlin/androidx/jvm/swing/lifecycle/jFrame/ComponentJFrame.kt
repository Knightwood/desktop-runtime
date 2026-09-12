package androidx.jvm.swing.lifecycle.jFrame

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.viewmodel.MySavedStateViewModelFactory
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
import java.awt.GraphicsConfiguration

open class ComponentJFrame : LifecycleJFrame,
    ViewModelStoreOwner, HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner
{
    constructor() : super()
    constructor(gc: GraphicsConfiguration?) : super(gc)
    constructor(title: String?) : super(title)
    constructor(title: String?, gc: GraphicsConfiguration?) : super(title, gc)

    private val logger = logFor("ComponentSwingWindow")
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


    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        MySavedStateViewModelFactory
    }

    public override val defaultViewModelCreationExtras: CreationExtras
        get() {
            val extras = MutableCreationExtras()
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
            extras[VIEW_MODEL_STORE_OWNER_KEY] = this
            return extras
        }

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

    private fun ensureViewModelStore() {
        if (_viewModelStore == null) {
            _viewModelStore = ViewModelStore()
        }
    }

    @CallSuper
    override fun onCreate(savedInstanceState: SavedState?) {
        savedStateRegistryController.performRestore(savedState)
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: SavedState) {
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
        logger.debug("onSaveInstanceState - $outState")
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
    }
}
