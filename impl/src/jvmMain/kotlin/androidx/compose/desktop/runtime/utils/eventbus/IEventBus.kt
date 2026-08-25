package google.androidx.accompanist.eventbus

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.plus

/**
 * @property name 事件总线名称，用于区分实例
 */
open class IEventBus(val name: Any) : LifecycleEventObserver {
    private var lifecycleOwner: LifecycleOwner? = null
    var scope: CoroutineScope =
        CoroutineScope(Dispatchers.Default) + SupervisorJob() + CoroutineName("EventBusManager")

    /**
     * 使用[setLifecycleOwner]设置此类的生命周期跟随谁
     *
     * 比如把 Activity 作为 LifecycleOwner，那么当 Activity 销毁时，会自动移除所有事件总线。
     *
     * 比如 把 Fragment 作为 LifecycleOwner，那么当 Fragment 销毁时，会自动移除所有事件总线。
     */
    internal var hostLifecycleState: Lifecycle.State = Lifecycle.State.INITIALIZED
        get() {
            return if (lifecycleOwner == null) {
                Lifecycle.State.CREATED
            } else {
                field
            }
        }

    fun setLifecycleOwner(owner: LifecycleOwner) {
        if (owner == lifecycleOwner) {
            return
        }
        lifecycleOwner?.lifecycle?.removeObserver(this)
        lifecycleOwner = owner
        owner.lifecycle.addObserver(this)
        BusHolder.add(name, this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        hostLifecycleState = event.targetState
        if (event == Lifecycle.Event.ON_DESTROY) {
            source.lifecycle.removeObserver(this)
            destroy()
        }
    }

    open fun destroy() {
        lifecycleOwner = null
        scope.cancel("lifecycle destroyed", null)
        BusHolder.remove(name)
    }
}
