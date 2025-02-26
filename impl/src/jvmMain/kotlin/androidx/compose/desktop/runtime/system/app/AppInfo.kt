package androidx.compose.desktop.runtime.system.app

/**
 * 应用启动后，将应用信息放到这里，方便其他模块使用
 */
object AppInfoHolder {
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
    fun provide(scope: MutableAppInfo.() -> Unit) {
        val info = MutableAppInfo()
        scope.invoke(info)
        appInfo = info.toInMutableAppInfo()
    }

    override fun toString(): String {
       return appInfo.toString()
    }


}

data class AppInfo(
    val appName: String,
)

data class MutableAppInfo(
    var appName: String = "",
)

fun MutableAppInfo.toInMutableAppInfo(): AppInfo {
    return AppInfo(
        appName = appName
    )
}
