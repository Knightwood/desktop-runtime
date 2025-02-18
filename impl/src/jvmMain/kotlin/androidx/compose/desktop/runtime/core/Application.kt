package androidx.compose.desktop.runtime.core

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.activity.Intent
import com.github.knightwood.slf4j.kotlin.info
import com.github.knightwood.slf4j.kotlin.logger
import kotlinx.coroutines.*

/**
 * 单例，作用类似于android中的application
 */
open class Application : ApplicationBase() {
    private var aware: Array<out Aware> = emptyArray()

    /**
     * 若为true，则onCreate将使用协程初始化所有的aware逻辑块
     */
    protected var async: Boolean = false

    //内部使用
    internal var fake: Boolean = true
    private lateinit var lifecycleRegistry: LifecycleRegistry

    @CallSuper
    open fun onCreate() {
        val f: () -> Unit = {
            for (a in aware) {
                a.onCreate(this@Application)
            }
        }
        if (async) scope.launch { f.invoke() } else f.invoke()
        logger.info { "Application onCreate" }
    }

    @CallSuper
    open fun onDestroy() {
        logger.info { "Application onDestroy" }
    }

    internal fun prepare(aware: Array<out Aware>) {
        try {
            fake = false
            this.aware = aware
            startAllService()
            scope.launch {
                withContext(Dispatchers.Main) {
                    // 初始化时设置生命周期状态
                    lifecycleRegistry = LifecycleRegistry(this@Application)
                    lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
                    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                }
            }
            onCreate()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * 退出应用，作用同windowManager().exitApplication()，但是多了生命周期处理
     */
    internal fun exit() {
        scope.launch {
            withContext(Dispatchers.Main) {
                onDestroy()
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                windowManager().killApplication()
            }
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}

/**
 * 从这里可以得到全局的application引用，用于上下文操作
 */
@Volatile
internal var applicationInternal: Application = Application()
private val lock = Any()

//<editor-fold desc="与activity结合">
/**
 * 一切的开端；一切的终结；
 */
fun startApplication(
    mainActivity: Class<out Activity>,
    applicationClass: Class<out Application> = Application::class.java,
    vararg aware: Aware,
    intentBuilder: (Intent.() -> Unit)? = null
) {
    synchronized(lock) {
        if (applicationInternal.fake) {
            applicationInternal = applicationClass.getDeclaredConstructor().newInstance().also {
                it.prepare(aware)
                it.startMainThread(mainActivity, intentBuilder)
            }
        } else {
            throw IllegalStateException("Application already started")
        }
    }
}
//</editor-fold>
