package androidx.compose.desktop.runtime.core

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.compose.desktop.runtime.core.context.ContextWrapper
import androidx.compose.desktop.runtime.core.context.Context
import androidx.jvm.system.di.InstanceContext
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.jvm.system.di.InstanceKoinHelpers
import androidx.jvm.system.di.inject
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.LifecycleOwner
import com.github.knightwood.slf4j.kotlin.error
import com.github.knightwood.slf4j.kotlin.info
import com.github.knightwood.slf4j.kotlin.logFor
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.skiko.MainUIDispatcher
import org.koin.core.qualifier.named
import kotlin.system.exitProcess

private val logger = logFor("Application")

/**
 * 作用类似于android中的application
 */
open class Application : ContextWrapper(), LifecycleOwner, InstanceKoinComponent {
    private val mutex = Mutex()

    /**
     * 此协程最终会随着进程结束而结束，不必担心生命周期
     */
    val scope: CoroutineScope by inject<CoroutineScope>(named<Application>())

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    @Suppress("LeakingThis")
    var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    //<editor-fold desc="生命周期">

    fun attach(context: Context) {
        attachBaseContext(context)
    }

    /**
     * onCreate 方法的调用时机早于compose系统启动，晚于ActivityManager和WindowManager创建
     */
    @CallSuper
    open fun onCreate() {
        runOnUIThread(mutex) {
            lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
        logger.info { "Application onCreate" }
    }

    /**
     * 执行所有添加注册的[Aware]
     */
    private fun awareInvoke(
        awares: Array<out Aware> = arrayOf(),
    ) {
        if (awares.isEmpty()) return
        awares
            .groupBy { aware -> aware is AsyncAware }
            .forEach { (async, groupedAwares) ->
                if (async) {
                    scope.launch(Dispatchers.Unconfined) {
                        groupedAwares.forEach { aware ->
                            (aware as AsyncAware).onCreate(this@Application)
                        }
                    }
                } else {
                    groupedAwares.forEach { aware ->
                        (aware as SyncAware).onCreate(this@Application)
                    }
                }
            }
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
        awares: Array<out Aware>,
    ) {
        try {
            onCreate()
            awareInvoke(awares)
        } catch (e: Exception) {
            logger.error(throwable = e) { "An error was encountered" }
            throw e
        }
    }
    //</editor-fold>

    /**
     * 结束应用进程
     *
     * 所有activity销毁 -> 清理activityManager、WindowManager等 -> Application destroy
     */
    internal suspend fun release() {
        withContext(MainUIDispatcher) {
            try {
                // FIXME: 不知道为什么有时候他的生命周期状态会退回到`INITIALIZED`，但这不妨碍我们结束应用
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                onDestroy()
                activityManager().release()
                val windowMgr = windowManager()
                windowMgr.release()
                windowMgr.exitApplication()
            } catch (e: Exception) {
                logger.error(throwable = e) { "Current lifecycle state: ${lifecycleRegistry.currentState}" }
            } finally {
                InstanceContext.stopKoin()
                scope.cancel("Application existing...")
                exitProcess(0)
            }
        }
    }

    companion object {
        const val TAG = "ServiceInstanceProvider"
    }
}

/**
 * 在ui线程运行代码块
 *
 * @param lock 如果不为null，则运行代码块时，会先获取锁，然后运行代码块，最后释放锁
 * @param block 需要运行在ui线程的代码块
 */
fun Application.runOnUIThread(
    lock: Mutex? = null,
    block: suspend CoroutineScope.() -> Unit,
) {
    scope.launch(MainUIDispatcher) {
        lock?.let {
            it.withLock { block() }
        } ?: block()
    }
}

/**
 * 利用[Application.scope]在ui线程运行代码块
 *
 * @param lock 如果不为null，则运行代码块时，会先获取锁，然后运行代码块，最后释放锁
 * @param block 需要运行在ui线程的代码块
 */
fun runOnUIThread(
    lock: Mutex? = null,
    block: suspend CoroutineScope.() -> Unit,
) {
    val scope = InstanceKoinHelpers.getKoin().get<CoroutineScope>(named<Application>())
    scope.launch(MainUIDispatcher) {
        lock?.let {
            it.withLock { block() }
        } ?: block()
    }
}
