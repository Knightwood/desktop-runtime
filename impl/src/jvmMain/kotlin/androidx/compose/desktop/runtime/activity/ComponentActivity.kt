package androidx.compose.desktop.runtime.activity

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.compose.desktop.runtime.viewmodel.createVM
import kotlin.reflect.KClass

/**
 * 1. 数据的保存是在Activity的onSaveInstanceState()中调用了SavedStateRegistryController的performSave()方法来实现
 * 2. SavedStateRegistryController是SavedStateRegistry的控制类，关于数据的保存和恢复都转发给了该类处理，performSave()方法最终最后转交到SavedStateRegistry的performSave()中。
 * 3. performSave()主要是将需要保存的数据写入到Activity的Bundle对象实现
 * 4. 数据的恢复即在onCreate()调用了performRestore()方法，将保存的数据取出恢复
 * 5. 对于需要保存的数据，实现SavedStateProvider接口，注册一下需要保存的数据；取回数据时；外部通过使用和传给
 *    registerSavedStateProvider() 方法时一样的 key 来取数据，并在取了之后将数据从
 *    mRestoredState 中移除。
 * 6. ViewModel创建时默认已经实现了SavedStateProvider等接口，实现了数据保存时从ViewModel中获取数据，恢复时给ViewModel赋值。
 * 7. 存储的数据不能存超过1M
 *
 * ```
 * class TestViewModel(
 *     val savedStateHandle: SavedStateHandle,
 *     val i: Int
 * ) : ViewModel() {
 * }
 *
 * open class TestActivity : ComponentActivity() {
 *     val randoms = Random.nextInt(0, 11)
 *     var tag = "Activity$randoms"
 *     private val logger = logger(tag)
 *
 *     val one = object : CreationExtras.Key<Int> {}
 *     val vm: TestViewModel by viewModels<TestViewModel>(extrasProducer = {
 *         val extras = MutableCreationExtras()
 *         extras[one] = 2
 *         extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
 *         extras[VIEW_MODEL_STORE_OWNER_KEY] = this
 *         extras
 *     }, {
 *         object : ViewModelProvider.Factory {
 *             override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
 *                 return TestViewModel(
 *                     extras.createSavedStateHandle(),
 *                     extras[one] ?: 90
 *                 ) as T
 *             }
 *         }
 *     })
 *
 *      override fun onCreate() {
 *          super.onCreate()
 *          logger.info("onCreate：vm参数：" + vm.i)
 *          logger.info("onCreate：vm：" + vm)
 *      }
 *
 *     override fun onReStart(intent: Intent?) {
 *         super.onReStart(intent)
 *         logger.info("onReStart")
 *     }
 *
 *     override fun onPause() {
 *         super.onPause()
 *         logger.info("onPause")
 *     }
 *
 *     override fun onResume() {
 *         super.onResume()
 *         logger.info("onResume")
 *     }
 *
 *     override fun onStart() {
 *         super.onStart()
 *         logger.info("onStart")
 *     }
 *
 *     override fun onStop() {
 *         super.onStop()
 *         logger.info("onStop")
 *     }
 *
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         logger.info("onDestroy")
 *     }
 * }
 *
 * ```
 */
open class ComponentActivity : Activity(), ViewModelStoreOwner, HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

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
            check(application.fake) {
                ("Your activity is not yet attached to the " +
                        "Application instance. You can't request ViewModel before onCreate call.")
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

}
