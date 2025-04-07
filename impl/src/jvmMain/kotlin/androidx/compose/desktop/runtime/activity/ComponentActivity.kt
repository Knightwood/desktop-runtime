package androidx.compose.desktop.runtime.activity

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.domain.ProvideAndroidCompositionLocals
import androidx.compose.desktop.runtime.viewmodel.createVM
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import androidx.core.bundle.Bundle
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.launch
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
 *     private val logger = logFor(tag)
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
            check(!application.fake) {
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performRestore(bundle)
        super.onCreate(savedInstanceState)
    }

    override fun show() {
        if (mWindow.exit.value) {//已经退出，需要重建activity
            ActivityManager.register(uuid, this)
            lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            mWindow.active()
        } else {
            lifecycleScope.launch {
                mWindow.isHidden.value = (false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
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
    override fun onDestroy() {
        super.onDestroy()
        if (intent.clearSaveState){
            activityManager().clearBundle(uuid)
        }
    }
    /**
     * 创建一个ComposeView，并绑定生命周期。
     *
     * @param state 窗口状态，默认为[rememberWindowState]
     * @param title 窗口标题，默认为"Untitled"
     * @param icon 窗口图标，默认为null
     * @param closeActivity 点击窗口关闭按钮时，是否关闭Activity，默认为false
     * @param undecorated 是否无边框，默认为false
     * @param transparent 是否透明，默认为false
     * @param resizable 是否可resize，默认为true
     * @param enabled 是否启用，默认为true
     * @param focusable 是否可聚焦，默认为true
     * @param alwaysOnTop 是否一直置顶，默认为false
     * @param onPreviewKeyEvent 预处理按键事件，默认为{@code false}
     * @param onKeyEvent 处理按键事件，默认为{@code false}
     * @param content 窗口内容
     */
    @Composable
    fun ComposeView(
        state: WindowState = rememberWindowState(),
        title: String = "Untitled",
        icon: Painter? = null,
        closeActivity: Boolean = true,
        undecorated: Boolean = false,
        transparent: Boolean = false,
        resizable: Boolean = true,
        enabled: Boolean = true,
        focusable: Boolean = true,
        alwaysOnTop: Boolean = false,
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
        content: @Composable FrameWindowScope.() -> Unit
    ) {
        ProvideAndroidCompositionLocals(
            id = uuid.toString(),
            this,
            this@ComponentActivity,
            this@ComponentActivity,
            this@ComponentActivity
        ) {
            Window(
                onCloseRequest = if (closeActivity) ::finish else ::hide,
                state = state,
                visible = !mWindow.isHidden.value,
                title = title,
                icon = icon,
                undecorated = undecorated,
                transparent = transparent,
                resizable = resizable,
                enabled = enabled,
                focusable = focusable,
                alwaysOnTop = alwaysOnTop,
                onPreviewKeyEvent = onPreviewKeyEvent,
                onKeyEvent = onKeyEvent,
                content = {
                    val lc: LifecycleOwner = LocalLifecycleOwner.current
                    remember {
                        lc.lifecycle.addObserver(this@ComponentActivity)
                    }
                    content()
                }
            )
        }
    }
}
