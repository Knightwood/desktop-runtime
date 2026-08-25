package androidx.compose.desktop.runtime.context

import androidx.compose.desktop.runtime.activity.ActivityManager
import androidx.compose.desktop.runtime.core.intent.Intent
import androidx.compose.desktop.runtime.activity.ActivityResultCallback
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.window.WindowManager
import kotlin.reflect.KClass

open class ContextWrapper : Context() {
    private lateinit var mBase: Context
    protected fun attachBaseContext(base: Context) {
        check(!this::mBase.isInitialized) { "Base context already set" }
        mBase = base
    }

    override val application: Application
        get() = mBase.application
    override val applicationContext: Context
        get() = mBase.applicationContext

    override fun <T : Any> getService(cls: KClass<T>): T = mBase.getService<T>(cls)

    override fun windowManager(): WindowManager = mBase.windowManager()

    override fun activityManager(): ActivityManager = mBase.activityManager()

    override fun exitApp() = mBase.exitApp()

    override fun startActivity(
        intent: Intent,
    ) = mBase.startActivity(intent)

    override suspend fun startActivityForResult(
        intent: Intent,
        callback: ActivityResultCallback,
    ) = mBase.startActivityForResult(intent, callback)
}
