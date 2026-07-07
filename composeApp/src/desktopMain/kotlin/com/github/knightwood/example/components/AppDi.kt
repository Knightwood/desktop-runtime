package com.github.knightwood.example.components

import androidx.compose.desktop.runtime.utils.DefaultUncaughtExceptionHandler.icon
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.github.knightwood.example.components.datastore.getJsonDatastore
import com.github.knightwood.example.components.settings.RenderSettings
import com.github.knightwood.example.components.settings.RenderSettingsProvider
import com.github.knightwood.example.path.FilePathProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.awt.Window
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

object AppDi {
    val modules = module {
        single<DataStore<RenderSettings>>(named("xSettingsDS")) {
            getJsonDatastore(
                defaults = RenderSettingsProvider.defaultValue(),
                produceFile = {  FilePathProvider.publicConfigDir.resolve("xSettings.json").toFile() },
                corruptionHandler= ReplaceFileCorruptionHandler {
                    SwingUtilities.invokeLater {
                        val jFrame = JFrame("提示")
                        jFrame.iconImage = null
                        jFrame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                        buildPreferencesErrorDialog(jFrame)
                    }
                    RenderSettingsProvider.defaultValue()
                }
            )
        }
    }
}
fun buildPreferencesErrorDialog(
    parentComponent: Window,
) {
    JOptionPane.showConfirmDialog(
        parentComponent,
        "数据文件损坏，请检查磁盘",
        "提示",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.WARNING_MESSAGE,
        icon,
    ).also {
        parentComponent.dispose()
    }
}
