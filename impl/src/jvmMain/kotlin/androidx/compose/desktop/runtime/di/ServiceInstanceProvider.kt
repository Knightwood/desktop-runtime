package androidx.compose.desktop.runtime.di

import androidx.compose.desktop.runtime.activity.ActivityLauncher
import androidx.compose.desktop.runtime.activity.ActivityManager
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.core.intent.IntentProcessor
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
import androidx.compose.desktop.runtime.window.WindowManager
import androidx.jvm.system.di.InstanceContext
import androidx.jvm.system.di.ModuleProvider
import com.google.auto.service.AutoService
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.jetbrains.skiko.MainUIDispatcher
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

@AutoService(ModuleProvider::class)
class ServiceInstanceProvider : ModuleProvider {

    override fun provide(): org.koin.core.module.Module {
        return module {
            single {
                ActivityLauncher(it.get(),it.get())
            }
            singleOf(::ActivityManager)
            singleOf(::WindowManager)
            singleOf(::IntentProcessor)
            singleOf(::ApplicationSaveStateSaver)
            single(named<Application>()) {
                CoroutineScope(Dispatchers.Default) + SupervisorJob() + CoroutineName("Application")
            }
            single(named<ActivityManager>()) {
                CoroutineScope(MainUIDispatcher) + SupervisorJob() + CoroutineName("ActivityManager")
            }
        }
    }

    companion object {
        const val TAG = "ServiceInstanceProvider"
    }
}

/**
 * 获取某个服务的实例
 */
internal inline fun <reified T : Any> getServiceInstance(): T {
    return InstanceContext.get().get<T>()
}
