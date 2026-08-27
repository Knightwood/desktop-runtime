package androidx.compose.desktop.runtime.fragment

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.core.intent.Intent
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
import androidx.compose.desktop.runtime.savestate.Token
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.jvm.system.di.inject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedState
import com.github.knightwood.slf4j.kotlin.info
import com.github.knightwood.slf4j.kotlin.kLogger
import java.lang.ref.WeakReference

/**
 * 无ui的通用生命周期组件,提供如下功能:
 * 1. 生命周期\同步父组件生命周期,生命周期事件回调
 * 2. ViewModelStoreOwner
 * 3. SavedStateRegistryOwner\自动保存恢复状态
 *
 * ```
 * open class ScreenComponent() : LifecycleComponent() {
 *
 * }
 *
 * val screen = ScreenComponent()
 * screen.attach(Token("component_1"),lifecycleOwner.lifecycle)
 * 如果调用attach时传入了hostLifecycle,则会在hostLifecycle走到onDestroy时触发状态保存
 * 如果未传入hostLifecycle,则需要手动调用screen.onDestroy()以保存状态
 * ```
 */
abstract class LifecycleComponent() :
    LifecycleOwner,
    InstanceKoinComponent {
    val stateSaver by inject<ApplicationSaveStateSaver>()
    internal var token: Token? = null

    /**
     * 在[ApplicationSaveStateSaver]中使用token注册一个SaveState,用于存放所有需要保存的状态
     * 状态栏会来自[onSaveInstanceState]、[androidx.savedstate.SavedStateRegistry]等
     * 调用此方法时需要确保已经给intent赋过值
     */
    internal val savedState: SavedState?
        get() {
            val id = token ?: return null
            return stateSaver.getSaveState(id)
        }
    private val finalId = Token(this::class.qualifiedName ?: this::class.hashCode().toString())

    /**
     * 上面的token与状态保存和恢复相关,如果不需要使用状态保存和恢复功能,则token为null,
     * 此时就无法使用token标识activity的唯一性了,因此需要一个回退字段标识唯一性.
     */
    protected val idn: Token get() = token ?: finalId

    private lateinit var hostLifecycle: WeakReference<Lifecycle>
    private val hostLifecycleObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            kLogger.info { "host lifecycle changed: $event" }
            if (event != Lifecycle.Event.ON_CREATE) {
                syncLife(event)
            }
            when (event) {
                ON_RESUME -> onResume()
                ON_PAUSE -> onPause()
                ON_STOP -> onStop()
                ON_DESTROY -> {
                    deSyncHostLifecycle()
                    onDestroy()
                }

                ON_START -> onStart()
                else -> {}
            }
        }
    }

    @Suppress("LeakingThis")
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry


    /**
     * 生成实例后调用此方法开始生命周期流程
     *
     * @param token 保存状态的关联id,若传入null,则不使用状态保存功能
     * @param hostLifecycle 父组件生命周期(可选).用于将当前组件实例生命周期关联到父组件的生命周期
     */
    open fun attach(token: Token? = null, hostLifecycle: Lifecycle? = null) {
        this.token = token
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        onCreate(savedState)
        //如果有传入父组件生命周期,则同步父组件生命周期到当前实例
        hostLifecycle?.let {
            attachHostLifecycle(it)
        }
    }

    fun isAttachedHostLifecycle(): Boolean {
        return (this::hostLifecycle.isInitialized)
    }

    fun attachHostLifecycle(lifecycle: Lifecycle) {
        if (isAttachedHostLifecycle()) {
            throw IllegalStateException("Already attached to hostLifecycle")
        }
        this.hostLifecycle = WeakReference(lifecycle)
        lifecycle.addObserver(hostLifecycleObserver)
    }

    /**
     * sync input lifecycle event to current component instance
     * 如果在调用[attach]时没有传入父组件生命周期,则此组件的生命周期将永远停留在onCreated状态,使用者可以使用此方法手动更改组件实例的生命周期.
     */
    protected fun syncLife(event: Lifecycle.Event) {
        lifecycleRegistry.currentState = event.targetState
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    @CallSuper
    open fun onCreate(savedInstanceState: SavedState?) {
//        logger.info("恢复状态，uuid:$uuid")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    open fun onStart() {}

    @CallSuper
    open fun onReStart(intent: Intent? = null) {

    }

    @CallSuper
    open fun onPause() {
    }

    @CallSuper
    open fun onResume() {
    }

    @CallSuper
    open fun onStop() {
    }

    @CallSuper
    open fun onSaveInstanceState(outState: SavedState) {
//        logger.info("保存状态，uuid:$uuid")
    }

    @CallSuper
    open fun onDestroy() {
        savedState?.let { onSaveInstanceState(it) }
    }

    /**
     * 手动结束生命周期
     */
    open fun finish() {
        syncLife(Lifecycle.Event.ON_DESTROY)
        deSyncHostLifecycle()
        onDestroy()//必须要在同步生命周期前调用，否则lifecycleScope会结束，导致无法正常释放
    }

    /**
     * 断开与父组件的生命周期链接
     */
    protected fun deSyncHostLifecycle() {
        //移除对于宿主的生命周期监听
        hostLifecycle.get()?.removeObserver(hostLifecycleObserver)
        this.hostLifecycle.clear()
    }
}
