package androidx.compose.desktop.runtime.context

import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.activity.ActivityManager
import androidx.compose.desktop.runtime.activity.result.ActivityResultCallback
import androidx.compose.desktop.runtime.activity.Intent
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.window.WindowManager

open class ContextWrapper : IContext() {
    lateinit var mBase: IContext

    override val application: Application
        get() = mBase.application

    override fun windowManager(): WindowManager = mBase.windowManager()

    override fun activityManager(): ActivityManager = mBase.activityManager()

    override fun exitApp() {
        mBase.exitApp()
    }

    override fun startActivity(
        intent: Intent
    ) {
        mBase.startActivity(intent)
    }

    override fun startActivityForResult(
        intent: Intent,
        block: ActivityResultCallback
    ) {
        mBase.startActivityForResult(intent, block)
    }
}
