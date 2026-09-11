package androidx.jvm.system.core

import androidx.jvm.system.process.ProcessInfoHelper
import androidx.jvm.system.utils.JvmUtils
import androidx.jvm.system.utils.MutableLazy

/**
 * 应用启动后，将应用信息放到这里，方便其他模块使用
 *
 * 需要在使用[AppPathProvider]之前就初始化
 */
object AppInfoProvider {
    private var appInfo: AppInfo by MutableLazy {
        val info = ProcessInfoHelper.sampleInfo
        val isProcessNameIsJava = ProcessInfoHelper.sampleInfo.isProcessNameIsJava
        AppInfo(
            appName = if (isProcessNameIsJava) "DevJava" else info.processName
        )
    }

    /**
     * 获取应用信息
     */
    fun get(): AppInfo {
        return appInfo
    }

    /**
     * 提供应用信息
     */
    fun provide(scope: AppInfo.() -> Unit) {
        val info = AppInfo()
        scope.invoke(info)
        appInfo = info
    }

    override fun toString(): String {
        return appInfo.toString()
    }

}

/**
 * @property isDevMode 是否是开发模式，未打包运行
 * @property appName 软件名称
 * @property appDisplayName 软件显示名称、美化名称，给用户看的名称，默认与软件名称相同
 */
data class AppInfo(
    @Deprecated("无用")
    var isDevMode: Boolean = false,
    var appName: String = "untitled",
    @Deprecated("无用")
    var appDisplayName: String = appName,
)
