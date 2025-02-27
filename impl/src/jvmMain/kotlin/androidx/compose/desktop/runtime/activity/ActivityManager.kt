package androidx.compose.desktop.runtime.activity

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.jetbrains.skiko.MainUIDispatcher
import java.util.*

/**
 * 管理所有的activity
 */
object ActivityManager {
    const val NAME = "ActivityManager"
    val scope = CoroutineScope(MainUIDispatcher) + SupervisorJob() + CoroutineName("ActivityManager")
    private val activityMap: MutableMap<UUID, Activity> = mutableMapOf()

    /**
     * 好吧，目前没有可实现的
     */
    fun prepare() {

    }

    operator fun get(uuid: UUID?): Activity? {
        return activityMap[uuid]
    }

    /**
     * 如果activity使用标准模式，使用此方法将只能找到最早添加的实例
     */
    operator fun get(cls: Class<out Activity>): Activity? {
        return activityMap.values.find { it.javaClass == cls }
    }

    fun register(uuid: UUID, activity: Activity) {
        activityMap[uuid] = activity
    }

    fun remove(uuid: UUID) {
        activityMap.remove(uuid)
    }


    fun release() {
        activityMap.values.forEach {
            it.finish()
        }
        activityMap.clear()
    }


}
