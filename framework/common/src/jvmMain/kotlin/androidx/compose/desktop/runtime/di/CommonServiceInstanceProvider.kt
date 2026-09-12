package androidx.compose.desktop.runtime.di

import androidx.compose.desktop.runtime.core.ServiceBooter
import androidx.compose.desktop.runtime.core.intent.IntentProcessor
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
import androidx.jvm.system.di.InstanceContext
import androidx.jvm.system.di.ModuleProvider
import com.google.auto.service.AutoService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

@AutoService(ModuleProvider::class)
class CommonServiceInstanceProvider : ModuleProvider {

    override fun provide(): org.koin.core.module.Module {
        return module {
            singleOf(::IntentProcessor)
            singleOf(::ApplicationSaveStateSaver)
        }
    }
}
