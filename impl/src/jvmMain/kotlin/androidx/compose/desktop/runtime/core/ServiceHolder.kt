package androidx.compose.desktop.runtime.core

import androidx.compose.desktop.runtime.activity.ActivityManager
import androidx.compose.desktop.runtime.domain.RunningState
import androidx.compose.desktop.runtime.window.WindowManager
import androidx.jvm.system.di.InstanceContext
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 持有所有的管理工具，使用[get]函数获取管理工具
 */
internal object ServiceHolder {
    /**
     * 程序的运行状态
     */
    var runningState: MutableSharedFlow<RunningState> = MutableSharedFlow()

    /**
     * 生成并持有所有资源服务实例
     */
    fun prepare() {
        InstanceContext.get().run {
            get<ActivityManager>()
            get<WindowManager>()
        }
    }

    fun release() {

    }

}
