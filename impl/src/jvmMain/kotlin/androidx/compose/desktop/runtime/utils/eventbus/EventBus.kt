package google.androidx.accompanist.eventbus

import androidx.lifecycle.LifecycleOwner
import google.androidx.accompanist.eventbus.observe
import kotlinx.coroutines.CoroutineScope


fun example(owner: LifecycleOwner) {
    //获取默认的EventBus，其生命周期是[BusHolder]的生命周期。
    //等价于 EventBus.obtain(false)
    val defaultBus = EventBus.instance

    //获取全局生命周期的EventBus
    EventBus.obtain(false, "name")

    //获取创建过的EventBus，如果没有创建过，或者生命周期结束，则返回null
    val find: EventBus? = EventBus.get("bus_name")


    //创建/获取创建过的EventBus
    //bus_name用于区分不同的EventBus，以及让EventBus在一定程度上成为单例。
    //如果没有传入bus_name，则默认使用传入的[LifecycleOwner]作为name
    //等价的调用方式 EventBus(owner, false, "bus_name")
    val bus = EventBus.obtain(owner, false, "bus_name")

    //从该eventbus中获取一个信箱，发送或者接收事件
    bus["bottom_navigation"].apply {
        //向信箱中可以发送任意类型数据
        submit(1)
        observe {
            //收集信箱中的事件
        }
        observe<Int> {
            //前面可以向信箱中发送任意类型数据，这里仅收集信箱中类型为Int的事件
        }
    }
    //销毁指定的EventBus
    EventBus.destroy("bus_name")
    //销毁所有
    EventBus.destroyAll()
}

class EventBus private constructor(
    lifecycleOwner: LifecycleOwner,
    private val sticky: Boolean,
    name: Any,
) : IEventBus(name) {

    init {
        setLifecycleOwner(lifecycleOwner)
    }

    /**
     * 持有所有的事件总线。 Map<事件分组,信箱>
     */
    var map: MutableMap<String, IMailBox> = mutableMapOf()
    var factory: MailBoxFactory =
        MailBoxFactory { sticky, it -> FlowMailBox(it, FlowMailBox.Ini(sticky)) }

    /**
     * 获取一个信箱，如果信箱不存在，则使用[MailBoxFactory]创建一个信箱。
     *
     * @param key
     * @return
     */
    @JvmName("getIMailBox")
    operator fun get(key: String): IMailBox {
        return map.getOrPut(key) {
            factory.create(sticky, scope)
        }
    }

    /**
     * 获取一个信箱，如果信箱不存在，则使用[MailBoxFactory]创建一个信箱。
     *
     * @param key
     * @return
     */
    @JvmName("getMailBox")
    fun <T : IMailBox> get(key: String): T {
        return this[key] as T
    }

    /**
     * 为了支持不同的信箱实现（比如flow、livedata），需要传入一个工厂，来创建信箱
     */
    fun interface MailBoxFactory {
        fun create(sticky: Boolean, scope: CoroutineScope): IMailBox
    }

    fun setMailBoxFactory(factory: MailBoxFactory) {
        this.factory = factory
    }

    companion object {
        /**
         * 代替构造函数，获取一个已存在/新的EventBus实例
         *
         * @param lifecycleOwner eventbus是跟随lifecycleOwner生命周期的。
         * * 第一次调用[obtain]传入的是Fragment的生命周期，那么当Fragment销毁时，EventBus也会销毁。
         * * 第二次调用[obtain]传入的是Activity的生命周期，那么当Activity销毁时，EventBus会销毁。
         * * 如果两次调用传入的Lifecycle不同，且name也不同，则得到的是两个不同的实例。
         *
         * @param sticky
         *    是否使用粘性事件特性，只在创建EventBus实例时有用。如果name关联的EventBus实例已经存在，则这个参数无用。
         * @param name EventBus的name，用于区分不同的EventBus。默认是传入的lifecycleOwner
         * @return EventBus 返回已存在的实例，或者新创建一个实例。
         */
        fun obtain(
            lifecycleOwner: LifecycleOwner,
            sticky: Boolean,
            name: Any = lifecycleOwner
        ) = invoke(lifecycleOwner, sticky, name)

        /**
         * 作用同上面的[obtain]，只是传入的lifecycleOwner是[BusHolder]，相当于得到的都是全局的EventBus。
         *
         * @param sticky
         *    是否使用粘性事件特性，只在创建EventBus实例时有用。如果name关联的EventBus实例已经存在，则这个参数无用。
         * @param name
         */
        fun obtain(
            sticky: Boolean,
            name: Any = BusHolder
        ) = invoke(BusHolder, sticky, name)

        /**
         * 获取默认的EventBus实例。相当于调用obtain(sticky=false,name=BusHolder)。
         * 是一个特殊的全局EventBus实例。
         */
        val instance: EventBus
            get() = invoke(BusHolder, false, BusHolder)

        /**
         * 销毁指定的EventBus
         *
         * @param name eventbus的name
         */
        fun destroy(name: Any) {
            BusHolder.get(name)?.destroy()
        }

        /**
         * 销毁所有EventBus
         */
        fun destroyAll() {
            BusHolder.release()
        }

        //<editor-fold desc="operator fun ">
        operator fun invoke(
            lifecycleOwner: LifecycleOwner,
            sticky: Boolean,
            name: Any = lifecycleOwner
        ): EventBus {
            return (BusHolder.get(name) as? EventBus) ?: EventBus(lifecycleOwner, sticky, name)
        }

        /**
         * 从[BusHolder]中获取已存在的EventBus实例
         *
         * @param name
         * @return name对应的EventBus实例或者null
         */
        @JvmName("getFlowBusInstance")
        operator fun get(name: Any): EventBus? {
            return BusHolder.get(name) as? EventBus
        }

        //</editor-fold>

    }
}
