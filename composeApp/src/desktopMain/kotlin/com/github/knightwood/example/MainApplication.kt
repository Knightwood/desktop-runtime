package com.github.knightwood.example

import androidx.compose.desktop.runtime.core.Application
import androidx.jvm.system.core.AppInfoProvider
import androidx.jvm.system.core.AppPathProvider
import androidx.jvm.system.core.keepDirExist
import androidx.jvm.system.di.InstanceContext
import androidx.jvm.system.di.startUp
import androidx.jvm.system.process.ProcessLocker
import androidx.jvm.system.utils.SystemProperty
import ch.qos.logback.classic.LoggerContext
import com.github.knightwood.example.components.AppDi
import com.github.knightwood.example.components.AppStateHolder
import com.github.knightwood.example.components.render.SkikoPropertiesHelper
import com.github.knightwood.example.components.settings.XSettingsProvider
import com.github.knightwood.slf4j.kotlin.logFor
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.Level.DEBUG
import org.koin.core.logger.Level.ERROR
import org.koin.core.logger.Level.INFO
import org.koin.core.logger.Level.NONE
import org.koin.core.logger.Level.WARNING
import org.koin.core.logger.MESSAGE
import org.slf4j.LoggerFactory
import java.util.*


class MainApplication : Application() {
    private val logger = logFor("application")
    override fun onCreate() {
        super.onCreate()
        ctx = this
        //通过修改locale，修改软件的语言显示
        Locale.setDefault(Locale.US)
        InstanceContext.startUp()
        AppInfoProvider.provide {
            appName = "测试"
            isDevMode = false
        }
        startKoin {
            logger(object : org.koin.core.logger.Logger() {
                override fun display(level: Level, msg: MESSAGE) {
                    when (level) {
                        DEBUG -> logger.debug(msg)
                        INFO -> logger.info(msg)
                        ERROR -> logger.error(msg)
                        NONE -> logger.info(msg)
                        WARNING -> logger.warn(msg)
                    }
                }
            })
            modules(AppDi.modules)
        }
//        val lockfile = AppPathProvider.provider.internalConfigDirPath
//            .keepDirExist()
//            .resolve("lockfile.lock").toNioPath()
//        ProcessLocker.lock(lockfile)
        AppStateHolder.registerExitAction { exitApp() }
//        AppPathProvider.provider.print()
        runBlocking {
            SkikoPropertiesHelper.changeRenderApi(XSettingsProvider.query().skikoRenderApi)
            if (XSettingsProvider.read().singleInstance) {
                AppStateHolder.probe(::exitApp)
            }
        }
        testPath()
    }

    private fun testPath() {
        SystemProperty.get("user.dir")?.let {
            println("user.dir : $it")
        }
        SystemProperty["user.home"]?.let {
            println("user.home : $it")
        }
        SystemProperty.get("compose.application.resources.dir")?.let {
            println("compose.application.resources.dir : $it")
        }
        SystemProperty.get("skiko.library.path")?.let {
            println("skiko.library.path : $it")
        }
        SystemProperty.get("java.home")?.let {
            println("java.home : $it")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // 程序结束时，手动刷新日志
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.stop() // 确保所有日志都被刷新
        ProcessLocker.unlock()
    }

    companion object {
        lateinit var ctx: Application
    }
}
