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
        intent: Intent ,
    ) {
    }

    open fun startActivityForResult(
        intent: Intent,
        block: ActivityResultCallback
    ) {
    }
}
