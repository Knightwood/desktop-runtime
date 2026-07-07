package androidx.compose.desktop.runtime.di

import androidx.compose.desktop.runtime.activity.ActivityManager
import androidx.compose.desktop.runtime.window.WindowManager
import androidx.jvm.system.di.ModuleProvider
import com.google.auto.service.AutoService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

@AutoService(ModuleProvider::class)
class ServiceInstanceProvider : ModuleProvider {

    override fun provide(): org.koin.core.module.Module {
        return module {
            singleOf(::ActivityManager)
            singleOf(::WindowManager)
        }
    }
}
