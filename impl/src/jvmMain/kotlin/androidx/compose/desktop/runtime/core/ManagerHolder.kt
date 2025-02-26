package androidx.compose.desktop.runtime.core

import androidx.compose.desktop.runtime.activity.ActivityManager
import androidx.compose.desktop.runtime.domain.RunningState
import androidx.compose.desktop.runtime.window.WindowManager
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 持有所有的管理工具，使用[get]函数获取管理工具
 */
internal object ManagerHolder {
    /**
     * 程序的运行状态
     */
    var runningState: MutableSharedFlow<RunningState> = MutableSharedFlow()
    private val map: MutableMap<String, Any> = mutableMapOf()

    /**
     * 启动并配置好所有的管理服务
     */
    fun startAll() {
        // ActivityManager
        map[ActivityManager.NAME] = ActivityManager
        // WindowManager
        val windowManager = WindowManager.instance()
        map[WindowManager.NAME] = windowManager
        println("启动所有服务完成")
    }

    /**
     * 获取WindowManager
     *
     * ```
     * val manager= ManagerHolder[WindowManager.name]
     * ```
     */
    operator fun <T> get(name: String): T? {
        return map[name] as? T
    }
}
