package androidx.compose.desktop.runtime.activity

import androidx.core.bundle.Bundle
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.jetbrains.skiko.MainUIDispatcher

interface IBundleHolder {
    fun obtainBundle(uuid: String): Bundle
    fun obtainBundleNullable(uuid: String): Bundle?
    fun clearBundle(uuid: String)
    fun clear()
}

class BundleHolder :IBundleHolder{
    /**
     * 存储SaveState的bundle
     */
    val bundleSaverMap: MutableMap<String, Bundle> = mutableMapOf()

    override fun obtainBundle(uuid: String): Bundle {
        return bundleSaverMap.getOrPut(uuid) { Bundle() }
    }

    override fun obtainBundleNullable(uuid: String): Bundle? {
        return bundleSaverMap[uuid]
    }

    override fun clearBundle(uuid: String) {
        bundleSaverMap.remove(uuid)
    }
    override fun clear() {
        bundleSaverMap.clear()
    }
}

/**
 * 管理所有的activity
 */
object ActivityManager : IBundleHolder by BundleHolder() {
    const val NAME = "ActivityManager"
    val scope = CoroutineScope(MainUIDispatcher) + SupervisorJob() + CoroutineName("ActivityManager")
    private val activityMap: MutableMap<String, Activity> = mutableMapOf()

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
