package androidx.jvm.system.core.di

import androidx.jvm.system.core.AppBasePathProvider
import androidx.jvm.system.core.WindowsAppPathProvider
import androidx.jvm.system.di.InstanceContext
import androidx.jvm.system.di.ModuleProvider
import androidx.jvm.system.utils.SystemOs
import com.github.knightwood.slf4j.kotlin.kLogger
import com.github.knightwood.slf4j.kotlin.logFor
import com.google.auto.service.AutoService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val logger = logFor("WinServiceProvider")
@AutoService(ModuleProvider::class)
internal open class WinPlatformServiceProvider : ModuleProvider {
    override fun provide(): Module {
        logger.info("win")
        return module {
            single<AppBasePathProvider>(named(SystemOs.Windows)) {
                WindowsAppPathProvider()
            }
        }
    }
}
