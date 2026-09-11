package androidx.compose.desktop.runtime.core.context

import androidx.compose.desktop.runtime.activity.*
import androidx.compose.desktop.runtime.core.intent.Intent
import androidx.compose.desktop.runtime.activity.ActivityResultCallback
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.core.Singularity
import androidx.compose.desktop.runtime.window.WindowManager
import androidx.jvm.system.di.InstanceKoinComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.qualifier.named
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.suspendCoroutine
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
    override val applicationCoroutineScope: CoroutineScope
        get() = getKoin().get<CoroutineScope>(named<Application>())
    override val mainCoroutineScope: CoroutineScope
        get() = getKoin().get<CoroutineScope>(named<ActivityManager>())

    override fun exitApp() {
        Singularity.exitAppOrProcess(false)
    }

    override fun startActivity(
        intent: Intent,
    ) {
        activityManager().launchActivity(intent)
    }

    /**
     * 在[ActivityManager.scope]中生成并运行activity，如此，activity就跑在ui（主）线程上
     */
    override suspend fun startActivityForResult(
        intent: Intent,
        callback: ActivityResultCallback,
    ) {
        activityManager().launchActivity(intent)
        intent.collectActivityResult {
            callback.invoke(it.resultCode, it.data)
        }
    }

    override suspend fun startActivityForResult(intent: Intent): ActivityResult? {
        activityManager().launchActivity(intent)
        return intent.activityResultFlow.firstOrNull()
    }

    companion object {
        fun createBaseContextForApplication(instance: Application): ContextImpl {
            return ContextImpl().also { it.attachApplication(instance) }
        }

        fun createBaseContextForActivity(instance: Application = Singularity.applicationInternal): ContextImpl {
            return ContextImpl().also { it.attachApplication(instance) }
        }
    }
}
