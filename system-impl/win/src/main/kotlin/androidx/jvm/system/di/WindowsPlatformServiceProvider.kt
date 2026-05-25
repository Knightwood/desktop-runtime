package androidx.jvm.system.di

import androidx.jvm.system.core.AppBasePathProvider
import androidx.jvm.system.core.WindowsAppPathProvider
import androidx.jvm.system.utils.SystemOs
import androidx.jvm.system.windows.PlatformWindowHelper
import androidx.jvm.system.windows.WindowsWindowHelper
import com.github.knightwood.slf4j.kotlin.logFor
import com.google.auto.service.AutoService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val logger = logFor("WinServiceProvider")

@AutoService(ModuleProvider::class)
internal open class WindowsPlatformServiceProvider : ModuleProvider {
    private val idn = named(SystemOs.Windows)

    override fun provide(): Module {
        logger.info("win")
        return module {
            single<AppBasePathProvider>(idn) {
                WindowsAppPathProvider()
            }
            single<PlatformWindowHelper>(idn) {
                WindowsWindowHelper()
            }
        }
    }
}
