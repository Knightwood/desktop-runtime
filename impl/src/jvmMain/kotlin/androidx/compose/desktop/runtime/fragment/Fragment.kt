//package androidx.compose.desktop.runtime.fragment
//
//import androidx.compose.desktop.runtime.viewmodel.createVM
//import androidx.lifecycle.*
//import androidx.lifecycle.viewmodel.CreationExtras
//import androidx.savedstate.SavedStateRegistry
//import androidx.savedstate.SavedStateRegistryController
//import androidx.savedstate.SavedStateRegistryOwner
//import kotlin.reflect.KClass
//
//
///**
// * 包装DialogWindow
// *
// * ```
// * var isDialogOpen by remember { mutableStateOf(false) }
// *
// * Button(onClick = { isDialogOpen = true }) {
// *     Text(text = "Open dialog")
// * }
// *
// * if (isDialogOpen) {
// *     DialogWindow(
// *         onCloseRequest = { isDialogOpen = false },
// *         state = rememberDialogState(position = WindowPosition(Alignment.Center))
// *     ) {
// *         // Content of the window
// *     }
// * }
// * ```
// */
//abstract class Fragment : ViewModelStoreOwner, LifecycleOwner,
//    HasDefaultViewModelProviderFactory, SavedStateRegistryOwner {
//
//    @Suppress("LeakingThis")
//    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this@Fragment)
//    override val lifecycle: Lifecycle
//        get() = lifecycleRegistry
//
//    private var _viewModelStore: ViewModelStore? = null
//    override val viewModelStore: ViewModelStore
//        /**
//         * Returns the [ViewModelStore] associated with this activity
//         *
//         * @return a [ViewModelStore]
//         * @throws IllegalStateException if called before the Activity is attached
//         *    to the Application instance i.e., before onCreate()
//         */
//        get() {
//            check(application.fake) {
//                ("Your activity is not yet attached to the " +
//                        "Application instance. You can't request ViewModel before onCreate call.")
//            }
//            ensureViewModelStore()
//            return _viewModelStore!!
//        }
//
//    @Suppress("LeakingThis")
//    private val savedStateRegistryController: SavedStateRegistryController =
//        SavedStateRegistryController.create(this)
//
//    override val savedStateRegistry: SavedStateRegistry
//        get() = savedStateRegistryController.savedStateRegistry
//
//    init {
//        @Suppress("LeakingThis")
//        lifecycle.addObserver(object : LifecycleEventObserver {
//            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
//                if (event == Lifecycle.Event.ON_DESTROY) {
//                    viewModelStore.clear()
//                }
//            }
//        })
//        savedStateRegistryController.performAttach()
//        enableSavedStateHandles()
//        savedStateRegistryController.performRestore(null)
//    }
//
//    private fun ensureViewModelStore() {
//        if (_viewModelStore == null) {
//            _viewModelStore = ViewModelStore()
//        }
//    }
//
//    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
//        object : ViewModelProvider.Factory {
//            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
//                return createVM(modelClass.java, extras)
//            }
//        }
//    }
//
//}
