package androidx.compose.desktop.runtime.activity

import androidx.core.bundle.Bundle
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.jetbrains.skiko.MainUIDispatcher

class BundleHolder {
    /**
     * 存储SaveState的bundle
     */
    val bundleSaverMap: MutableMap<String, Bundle> = mutableMapOf()

    fun obtainBundle(uuid: String): Bundle {
        return bundleSaverMap.getOrPut(uuid) { Bundle() }
    }

    fun clearBundle(uuid: String) {
        bundleSaverMap.remove(uuid)
    }
}

/**
 * 管理所有的activity
 */
object ActivityManager {
    const val NAME = "ActivityManager"
    val scope = CoroutineScope(MainUIDispatcher) + SupervisorJob() + CoroutineName("ActivityManager")
    private val activityMap: MutableMap<String, Activity> = mutableMapOf()

    /**
     * 存储activity SaveState的bundle
     */
    val activitySavedBundle = BundleHolder()

    fun obtainBundle(uuid: String) = activitySavedBundle.obtainBundle(uuid)
    fun clearBundle(uuid: String) = activitySavedBundle.clearBundle(uuid)

    /**
     * 好吧，目前没有可实现的
     */
    fun prepare() {

    }

    operator fun get(uuid: String?): Activity? {
        return activityMap[uuid]
    }

    /**
     * 如果activity使用标准模式，使用此方法将只能找到最早添加的实例
     */
    operator fun get(cls: Class<out Activity>): Activity? {
        return activityMap.values.find { it.javaClass == cls }
    }

    fun register(uuid: String, activity: Activity) {
        activityMap[uuid] = activity
    }

    fun remove(uuid: String) {
        activityMap.remove(uuid)
    }


    fun release() {
        activityMap.values.forEach {
            it.finish()
        }
        activityMap.clear()
    }


}
