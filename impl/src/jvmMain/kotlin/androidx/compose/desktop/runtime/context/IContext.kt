package androidx.compose.desktop.runtime.context

import androidx.compose.desktop.runtime.activity.*
import androidx.compose.desktop.runtime.activity.result.ActivityResultCallback
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.window.WindowManager
import androidx.compose.runtime.staticCompositionLocalOf

val LocalContext = staticCompositionLocalOf<IContext> {
    noLocalProvidedFor("LocalContext")
}

fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}

/**
 * 仿照android，context将提供很多的系统的api或是英勇的快捷操作
 */
abstract class IContext {
    abstract val application: Application
    abstract fun windowManager(): WindowManager
    abstract fun activityManager(): ActivityManager

    /**
     * 结束软件生命
     */
    open fun exitApp() {}

    open fun startActivity(
        intent: Intent,
    ) {
    }

    open fun startActivityForResult(
        intent: Intent,
        block: ActivityResultCallback,
    ) {
    }
}

fun <DATA> IContext.startActivity(
    to: Class<out Activity>,
    data: DATA? = null,
    launchMode: LaunchMode = LaunchMode.STANDARD,
) {
    startActivity(Intent(to, data, launchMode))
}

fun <DATA> IContext.startActivity(
    to: Class<out Activity>,from: Any,
    data: DATA?,
    launchMode: LaunchMode = LaunchMode.STANDARD,
) {
    startActivity(Intent(from, to, data, launchMode))
}

fun <DATA> IContext.startActivity(
    to: Class<out Activity>,
    from: Class<*>,
    pair: Pair<LaunchMode, DATA?>? = null,
) {
    val (launchMode, data) = pair ?: Pair(LaunchMode.STANDARD, null)
    startActivity(Intent(from, to, data, launchMode))
}

fun <DATA> IContext.startActivity(
    to: Class<out Activity>,
    pair: Pair<LaunchMode, DATA?>? = null,
) {
    val (launchMode, data) = pair ?: Pair(LaunchMode.STANDARD, null)
    startActivity(Intent(to, data, launchMode))
}
