package androidx.compose.desktop.runtime.core

import androidx.jvm.system.di.InstanceContext
import androidx.jvm.system.di.startUp
import org.koin.core.error.KoinApplicationAlreadyStartedException
import org.koin.core.logger.Level
import org.koin.core.logger.Level.ERROR
import org.koin.core.logger.Level.INFO
import org.koin.core.logger.Level.NONE
import org.koin.core.logger.Level.WARNING
import org.koin.core.logger.MESSAGE
import org.koin.dsl.koinApplication
import org.slf4j.Logger
import kotlin.reflect.KClass

object ServiceBooter {

    /**
     * 启动koin加载所有内部模块、加载ModuleProvider接口实例提供的模块
     */
    fun bootstrap(logger: Logger) {
        //启动koin
        try {
            InstanceContext.startUp {
                logger(object : org.koin.core.logger.Logger() {
                    override fun display(level: Level, msg: MESSAGE) {
                        when (level) {
                            Level.DEBUG -> logger.debug(msg)
                            INFO -> logger.info(msg)
                            ERROR -> logger.error(msg)
                            NONE -> logger.info(msg)
                            WARNING -> logger.warn(msg)
                        }
                    }
                })
            }
        } catch (e: KoinApplicationAlreadyStartedException) {
            logger.warn(e.message, e)
        }
    }

    /**
     * 获取内部koin实例
     */
    fun koin() = InstanceContext.get()

    /**
     * 获取一个已注册到koin的框架服务实例
     *
     * 例如：
     * ```
     * // application结束时将状态保存到磁盘
     * // 这里只是为了演示，只打印了一下
     * getService(ApplicationSaveStateSaver::class)
     *     .export()
     *     .also {
     *         logger.info("Application save state saver changed to $it")
     *     }
     * ```
     */
    fun <T : Any> getService(cls: KClass<T>): T {
        return koin().get(cls)
    }

    fun <T : Any> getService(cls: Class<T>): T {
        return koin().get(cls.kotlin)
    }
}

/**
 * 获取某个服务的实例
 * ```
 * //公共：
 * getServiceInstance<ApplicationSaveStateSaver>()
 * getServiceInstance<IntentProcessor>()
 *
 * //compose:
 * getServiceInstance<ActivityManager>()
 * getServiceInstance<ActivityLauncher>()
 * getServiceInstance<WindowManager>()
 *
 * //swing:
 * getServiceInstance<JFrameManager>()
 * getServiceInstance<JFrameLauncher>()
 * ```
 */
inline fun <reified T : Any> getServiceInstance(): T {
    return ServiceBooter.getService(T::class)
}
