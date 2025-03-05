package com.github.knightwood.example

import ch.qos.logback.classic.LoggerContext
import androidx.compose.desktop.runtime.core.Application
import androidx.jvm.system.core.AppInfoProvider
import androidx.jvm.system.core.AppPathProvider
import org.slf4j.LoggerFactory
import java.util.Locale


class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //通过修改locale，修改软件的语言显示
        Locale.setDefault(Locale.CHINA)
        AppInfoProvider.provide {
            appName = "测试"
        }
        AppPathProvider.getInstance().print()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 程序结束时，手动刷新日志
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.stop() // 确保所有日志都被刷新
    }
}
