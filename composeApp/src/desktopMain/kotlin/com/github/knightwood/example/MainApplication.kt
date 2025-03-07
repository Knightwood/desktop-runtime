package com.github.knightwood.example

import androidx.compose.desktop.runtime.core.Application
import androidx.jvm.system.core.AppInfoProvider
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import java.util.*


class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ctx = this
        //通过修改locale，修改软件的语言显示
        Locale.setDefault(Locale.US)
        AppInfoProvider.provide {
            appName = "测试"
            isDevMode = true
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // 程序结束时，手动刷新日志
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.stop() // 确保所有日志都被刷新
    }

    companion object {
        lateinit var ctx: Application
    }
}
