package com.github.knightwood.example

import ch.qos.logback.classic.LoggerContext
import androidx.compose.desktop.runtime.core.Application
import org.slf4j.LoggerFactory


class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 程序结束时，手动刷新日志
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.stop() // 确保所有日志都被刷新
    }
}
