package androidx.compose.desktop.runtime.utils

import androidx.compose.desktop.runtime.system.utils.SystemOs
import androidx.compose.desktop.runtime.system.utils.SystemProperty
import androidx.compose.desktop.runtime.system.utils.currentOS
import androidx.compose.desktop.runtime.system.utils.getWindowsVersion


val systemVer by lazy {
    when (currentOS) {
        SystemOs.MacOS -> {
            SystemProperty.javaVer()
        }

        SystemOs.Windows -> {
            val osName = SystemProperty.os().lowercase()
            getWindowsVersion(osName, SystemProperty.javaVer())
        }

        SystemOs.Linux -> {
            ""
//            LinuxPlatform.getOsVersion()
        }
    }
}
