package androidx.compose.desktop.runtime.fragment

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
import androidx.compose.desktop.runtime.savestate.Token
import androidx.compose.desktop.runtime.viewmodel.createVM
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.jvm.system.di.inject
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.github.knightwood.slf4j.kotlin.info
import com.github.knightwood.slf4j.kotlin.kLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.getValue
import kotlin.reflect.KClass

/**
 * 提供了生命周期、ViewModelStoreOwner、SavedStateRegistryOwner等基础组件
 * 可用于作为一个ui片段的容器，类似于fragment
 *
 * ```
 * open class ScreenComponent() : IScreenComponent() {
 *
 * }
 *
 * val screen = ScreenComponent()
 * screen.prepare(lifecycleOwner.lifecycle)
 * ```
 */
abstract class IScreenComponent(val token: Token? = null) : ViewModelStoreOwner,
    LifecycleOwner, LifecycleEventObserver,
    HasDefaultViewModelProviderFactory, SavedStateRegistryOwner,
    InstanceKoinComponent {
    val stateSaver by inject<ApplicationSaveStateSaver>()

    /**
     * 在[ApplicationSaveStateSaver]中使用token注册一个SaveState,用于存放所有需要保存的状态
     * 状态栏会来自[onSaveInstanceState]、[SavedStateRegistry]等
     * 调用此方法时需要确保已经给intent赋过值
     */
    internal val savedState: SavedState?
        get() {
            val id = token ?: return null
            return stateSaver.getSaveState(id)
        }
    /**
     * 上面的token与状态保存和恢复相关,如果不需要使用状态保存和恢复功能,则token为null,
     * 此时就无法使用token标识activity的唯一性了,因此需要一个回退字段标识唯一性.
     */
    internal val idn: Token = Token(this.toString())

    // 组件是否已经释放
    val released: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private lateinit var parentLifecycle: WeakReference<Lifecycle>

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

    /**
     * 在生成实例后，调用此方法开始此类的生命周期流程
     */
    fun prepare(parentLifecycle: Lifecycle? = null) {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        onCreate(savedState)
        parentLifecycle?.let {
            this.parentLifecycle = WeakReference(it)
            it.addObserver(this)
        } ?: let {
            // todo 如果不同步父级生命周期，或许需要一些其他处理
        }
    }

    /**
     * observe parent lifecycle
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        kLogger.info { "parent lifecycle changed: $event" }
        if (event == Lifecycle.Event.ON_DESTROY) {
            endLife()
            onDestroy()
        }
        syncLife(event)
    }

    /**
     * sync activity lifecycle to fragment lifecycle
     */
    private fun syncLife(event: Lifecycle.Event) {
        lifecycleRegistry.currentState = event.targetState
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    @CallSuper
    open fun onCreate(savedInstanceState: SavedState?) {
//        logger.info("恢复状态，uuid:$uuid")
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    @CallSuper
    fun onSaveInstanceState(outState: SavedState) {
        savedStateRegistryController.performSave(outState)
//        logger.info("保存状态，uuid:$uuid")
    }

    @CallSuper
    open fun onDestroy() {
        savedState?.let { onSaveInstanceState(it) }
        lifecycleScope.launch {
            released.emit(true)
//            logger.info("释放组件，uuid:$uuid")
        }
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
        onDestroy()//必须要在同步生命周期前调用，否则lifecycleScope会结束，导致无法正常释放
        syncLife(Lifecycle.Event.ON_DESTROY)
        endLife()
    }

    private fun endLife() {
        //移除对于parent的生命周期监听
        this.parentLifecycle.get()?.removeObserver(this)
        this.parentLifecycle.clear()
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
