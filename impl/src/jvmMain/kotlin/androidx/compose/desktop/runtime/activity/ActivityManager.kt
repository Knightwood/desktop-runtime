package androidx.compose.desktop.runtime.activity

import androidx.savedstate.SavedState
import androidx.savedstate.savedState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.jetbrains.skiko.MainUIDispatcher

interface ISaveStateHolder {
    fun obtainSaveState(uuid: String): SavedState
    fun obtainSavestateNullable(uuid: String): SavedState?
    fun clearSaveState(uuid: String)
    fun clear()
    fun setSaveState(uuid: String, savedState: SavedState)
}

class SaveStateHolder : ISaveStateHolder {
    /**
     * 存储SaveState的bundle
     */
    val saveStateSaverMap: MutableMap<String, SavedState> = mutableMapOf()

    override fun obtainSaveState(uuid: String): SavedState {
        return saveStateSaverMap.getOrPut(uuid) { savedState() }
    }

    override fun obtainSavestateNullable(uuid: String): SavedState? {
        return saveStateSaverMap[uuid]
    }

    override fun clearSaveState(uuid: String) {
        saveStateSaverMap.remove(uuid)
    }

    override fun clear() {
        saveStateSaverMap.clear()
    }

    override fun setSaveState(uuid: String, savedState: SavedState) {
        saveStateSaverMap[uuid] = savedState
    }
}

/**
 * 管理所有的activity
 */
object ActivityManager : ISaveStateHolder by SaveStateHolder() {
    const val NAME = "ActivityManager"
    val scope = CoroutineScope(MainUIDispatcher) + SupervisorJob() + CoroutineName("ActivityManager")

    // activity map
    private val activityMap: MutableMap<String, Activity> = mutableMapOf()

    //任务栈
    internal val stack = mutableListOf<Activity>()
    val activityStack: List<Activity> get() = stack


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
        stack.add(activity)
    }

    fun remove(uuid: String) {
        activityMap.remove(uuid)
        stack.remove(activityMap[uuid])
    }


    fun release() {
        activityMap.values.forEach {
            it.finish()
        }
        activityMap.clear()
        stack.clear()
    }


}
