package androidx.compose.desktop.runtime.core

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.activity.Intent
import androidx.compose.desktop.runtime.context.ContextImpl
import androidx.compose.desktop.runtime.context.ContextWrapper
import androidx.compose.desktop.runtime.domain.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import androidx.lifecycle.LifecycleOwner
import com.github.knightwood.slf4j.kotlin.info
import com.github.knightwood.slf4j.kotlin.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.system.exitProcess

/**
 * 作用类似于android中的application
 */
open class Application : ContextWrapper(), LifecycleOwner {
    private var aware: Array<out Aware> = emptyArray()

    /**
     * 此协程最终会随着进程结束而结束，不必担心生命周期
     */
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default) + SupervisorJob() + CoroutineName("Application")

    init {
        mBase = ContextImpl()
    }

    /**
     * 若为true，则onCreate将使用协程初始化所有的aware逻辑块
     */
    protected var async: Boolean = false

    //内部使用
    internal var fake: Boolean = true

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    private lateinit var lifecycleRegistry: LifecycleRegistry

    //<editor-fold desc="生命周期">

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
    //</editor-fold>

    //<editor-fold desc="内部初始化">

    /**
     * 生成并配置Application-> 启动并配置activityManager、WindowManager等 ->MainActivity
     */
    internal fun prepare(
        aware: Array<out Aware>,
        applicationContent: @Composable ApplicationScope.() -> Unit = {},
    ) {
        try {
            fake = false
            this.aware = aware
            ServiceHolder.prepare()//启动所有的服务，比如窗口管理、activity管理
            windowManager().content = applicationContent
            scope.launch {
                withContext(Dispatchers.Main) {
                    // 初始化时设置生命周期状态
                    lifecycleRegistry = LifecycleRegistry(this@Application)
                    lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
                    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                }
            }
            scope.launch {
                ServiceHolder.runningState.collect{
                    when (it) {
                        is Stop -> {
                            logger.info("exit...")
                            applicationInternal.release()
                        }

                        else -> {}
                    }
                }
            }
            onCreate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 启动MainActivity，第一个显示出来的窗口
     */
    internal fun startMainActivity(mainActivity: Class<out Activity>, intentBuilder: (Intent.() -> Unit)?) {
        val intent = Intent()
        intentBuilder?.invoke(intent)
        startActivity(mainActivity, intent)//这里会运行在协程，注意调用时机
        windowManager().prepare()
    }
    //</editor-fold>

    /**
     * 结束应用进程
     *
     * 所有activity销毁 -> 清理activityManager、WindowManager等 -> Application destroy
     */
    internal fun release() {
        scope.launch {
            withContext(Dispatchers.Main) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                onDestroy()
                activityManager().release()
                windowManager().release()
                ServiceHolder.release()
                exitProcess(0)
            }
        }
    }
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
 *
 * @param aware 如果不想再application的onCreate函数中写太多逻辑，可以放到这里的初始化块
 * @param intentBuilder 启动主界面的参数
 * @param mainActivity 主界面
 * @param applicationClass 应用程序类，默认为Application
 */
inline fun <reified T : Activity, reified R : Application> startApplication(
    vararg aware: Aware,
    noinline intentBuilder: (Intent.() -> Unit)? = null
) {
    startApplication(
        T::class.java, R::class.java,
        *aware, intentBuilder = intentBuilder
    )
}

/**
 * 一切的开端；一切的终结；
 *
 * @param mainActivity 主界面
 * @param applicationClass 应用程序类，默认为Application
 * @param aware 如果不想再application的onCreate函数中写太多逻辑，可以放到这里的初始化块
 * @param applicationContent
 *    额外的UI代码块，不属于任何activity，会被applicationScope中被直接执行
 * @param intentBuilder 启动主界面的参数
 */
fun startApplication(
    mainActivity: Class<out Activity>,
    applicationClass: Class<out Application> = Application::class.java,
    vararg aware: Aware,
    applicationContent: @Composable ApplicationScope.() -> Unit = {},
    intentBuilder: (Intent.() -> Unit)? = null
) {
    synchronized(lock) {
        if (applicationInternal.fake) {
            applicationInternal = applicationClass.getDeclaredConstructor().newInstance().also {
                it.prepare(aware, applicationContent)
                it.startMainActivity(mainActivity, intentBuilder)
                //只要到达那个地方 it.exit() //如果都结束了，自然会走到这一步
            }
        } else {
            throw IllegalStateException("Application already started")
        }
    }
}
//</editor-fold>
