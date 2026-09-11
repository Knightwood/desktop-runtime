package androidx.jvm.system.di

import androidx.jvm.system.core.AppBasePathProvider
import androidx.jvm.system.core.MacosAppPathProvider
import androidx.jvm.system.utils.SystemOs
import com.google.auto.service.AutoService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

@AutoService(ModuleProvider::class)
internal open class MacPlatformServiceProvider : ModuleProvider {
    override fun provide(): Module {
        return module {
            single<AppBasePathProvider>(named(SystemOs.MacOS)) {
                MacosAppPathProvider()
            }
        }
    }
}
