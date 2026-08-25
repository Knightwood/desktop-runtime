package google.androidx.accompanist.eventbus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

/**
 * 支持两组api，一组使用内置协程作用域，另一组不用。
 */
interface IMailBox {

    fun submit(value: Any, delay: Duration = ZERO)
    fun observe(collector: FlowCollector<Any>)

    suspend fun post(value: Any, delay: Duration = ZERO)
    suspend fun collect(collector: FlowCollector<Any>)
}

data object NothingEvent : Any()


class FlowMailBox(val scope: CoroutineScope, ini: Ini) : IMailBox {
    var flow: MutableSharedFlow<Any> = MutableSharedFlow(
        replay = ini.replay,
        extraBufferCapacity = ini.extraBufferCapacity,
        onBufferOverflow = ini.onBufferOverflow
    )

    override fun submit(value: Any, delay: Duration) {
        scope.launch {
            if (delay > ZERO)
                delay(delay)
            post(value)
        }
    }

    override suspend fun post(value: Any, delay: Duration) {
        if (delay > ZERO)
            delay(delay)
        flow.emit(value)
    }

    override fun observe(collector: FlowCollector<Any>) {
        scope.launch { this@FlowMailBox.collect(collector) }
    }

    override suspend fun collect(collector: FlowCollector<Any>) {
        flow.collect(collector)
    }

    /**
     * flow的配置策略
     *
     * @property replay 表示在订阅时从流中回放的元素数量
     *
     * ```
     *      replay 0 代表不重放，也就是没有粘性
     *      replay n 如果设置为正整数 n，则在订阅时将向新订阅者回放最近的 n 个元素。
     * ```
     *
     * @property extraBufferCapacity 额外的缓冲容量，用于存储订阅者尚未消耗的元素。
     *
     * ```
     * 默认值为 0，表示不使用额外的缓冲容量。设置为正整数 m 时，会在内部使用一个带有额外 m 容量的缓冲区。
     * Flow 存在发送过快，消费太慢的情况，这种情况下，就需要使用缓存池，把未消费的数据存下来。
     * 缓冲池容量 = replay + extraBufferCapacity
     * ```
     *
     * @property onBufferOverflow 如果指定了有限的缓存容量，那么超过容量以后怎么办？ 表示在缓冲区溢出时的处理策略
     *
     * ```
     * BufferOverflow.SUSPEND ： 超过就挂起，默认实现
     * BufferOverflow.DROP_OLDEST : 丢弃最老的数据
     * BufferOverflow.DROP_LATEST : 丢弃最新的数据
     * ```
     */
    data class Ini(
        val replay: Int = 0,
        val extraBufferCapacity: Int = 0,
        val onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    ) : Serializable {
        constructor(sticky: Boolean) : this(if (sticky) 1 else 0)
    }
}

/**
 * 使用内置的协程去collect数据，且过滤掉不符合类型的数据
 *
 * @param collector
 * @param T
 */
inline fun <reified T> IMailBox.observe(collector: FlowCollector<T>) {
    observe {
        if (it is T) {
            collector.emit(it)
        }
    }
}

/**
 * 不使用内置的协程去collect数据，过滤掉不符合类型的数据
 *
 * @param collector
 * @param T
 */
suspend inline fun <reified T> IMailBox.collect(collector: FlowCollector<T>) {
    collect {
        if (it is T) {
            collector.emit(it)
        }
    }
}
