package androidx.compose.desktop.runtime.system.app

/**
 * 应用启动后，将应用信息放到这里，方便其他模块使用
 *
 * 需要在使用[AppPathProvider]之前就初始化
 */
object AppInfoProvider {
    private var appInfo: AppInfo? = null
    internal val SEARCH_WINDOW_TITLE
        get() = if (appInfo == null) {
            ""
        } else {
            appInfo!!.appName
        }

    /**
     * 获取应用信息
     */
    fun get(): AppInfo {
        return appInfo!!
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
 */
data class AppInfo(
    var isDevMode: Boolean = true,
    var appName: String = "untitled",
)
