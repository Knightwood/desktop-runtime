package google.androidx.accompanist.eventbus

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry


/**
 * 作为单例，且实现了[LifecycleOwner]，避免重复创建EventBus
 */
internal object BusHolder : LifecycleOwner {
    private val mLifecycleRegistry = LifecycleRegistry(this)
    private val map: MutableMap<Any, IEventBus> = mutableMapOf()

    init {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun add(name: Any, manager: IEventBus) {
        map[name] = manager
    }

    fun get(name: Any): IEventBus? {
        return map[name]
    }

    fun remove(name: Any) {
        map.remove(name)
    }

    fun release() {
        map.forEach {
            it.value.destroy()
        }
        map.clear()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override val lifecycle: Lifecycle
        get() = mLifecycleRegistry
}
