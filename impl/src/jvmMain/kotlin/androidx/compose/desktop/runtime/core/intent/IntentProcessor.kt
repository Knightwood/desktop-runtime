package androidx.compose.desktop.runtime.core.intent

import androidx.jvm.system.di.InstanceKoinComponent
import com.github.knightwood.slf4j.kotlin.logFor


fun interface IOperateIntentProcessor<T : OperateIntent> {
    fun process(intent: T): Boolean
}

private val logger = logFor("IOperateIntentProcessor")

/**
 * 将操作意图intent抽象为OperateIntent接口和具体的几个子类实现,
 * 此类用于识别intent类型,调用[process]方法时会将传入intent交给具体的某个类进行处理.
 *
 * 比如LaunchActivityIntent,专用于启动activity,此类[process]方法将其交给ActivityManager处理.
 * 各类能够处理intent的服务在启动后需要将自己注册到这里
 */
class IntentProcessor : InstanceKoinComponent, IOperateIntentProcessor<OperateIntent> {
    private val cache = mutableMapOf<Class<out OperateIntent>, (OperateIntent) -> Boolean>()

    /**
     * 注册一个intent处理服务,如果是相同类型的处理服务,后注册的将取代先注册的.
     * @param processor intent处理服务
     */
    internal inline fun <reified T : OperateIntent> registerProcessor(processor: IOperateIntentProcessor<T>) {
        /**
         * 将传入processor包装一层,实现安全的类型转换处理,避免Unchecked cast 警告
         */
        val bean: (OperateIntent) -> Boolean = { intent ->
            //不添加此注解也不会提示 Unchecked cast 警告
            @Suppress("UNCHECKED_CAST")
            processor.process(intent as T)
        }
        this.cache.put(T::class.java, bean)
        logger.debug("Registered ${processor.javaClass.simpleName}")
    }

    override fun process(intent: OperateIntent): Boolean {
        val foundProcessor = cache.get(intent::class.java) ?: return false
        return foundProcessor(intent)
    }
}
