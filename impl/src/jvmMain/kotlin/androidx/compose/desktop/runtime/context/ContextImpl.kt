package androidx.compose.desktop.runtime.context

import androidx.compose.desktop.runtime.activity.*
import androidx.compose.desktop.runtime.core.intent.Intent
import androidx.compose.desktop.runtime.activity.ActivityResultCallback
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.core.Singularity
import androidx.compose.desktop.runtime.window.WindowManager
import androidx.jvm.system.di.InstanceKoinComponent
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.KClass

open class ContextImpl() : Context(), InstanceKoinComponent {
    override lateinit var application: Application
    override lateinit var applicationContext: Context

    internal fun attachApplication(application: Application) {
        this.application = application
        this.applicationContext = application
    }

    override fun <T : Any> getService(cls: KClass<T>): T {
        return getKoin().get(cls)
    }

    override fun windowManager(): WindowManager = getKoin().get<WindowManager>()

    override fun activityManager(): ActivityManager = getKoin().get<ActivityManager>()

    override fun exitApp() {
        Singularity.exitAppOrProcess(false)
    }

    override fun startActivity(
        intent: Intent,
    ) {
        getKoin().get<ActivityManager>().launchActivity(intent)
    }
    /**
     * 在[ActivityManager.scope]中生成并运行activity，如此，activity就跑在ui（主）线程上
     */
    override suspend fun startActivityForResult(
        intent: Intent,
        callback: ActivityResultCallback,
    ) {
        coroutineScope {
            intent.collectResult {
                callback.invoke(it.resultCode, it.data)
            }
        }
        getKoin().get<ActivityManager>().launchActivity(intent)
    }

    companion object {
        fun createBaseContextForActivity(): Context {
            return ContextImpl()
        }
    }
}
