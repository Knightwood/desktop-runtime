package androidx.compose.desktop.runtime.utils

import androidx.jvm.system.utils.SystemOs
import androidx.jvm.system.utils.SystemProperty
import androidx.jvm.system.utils.currentOS
import androidx.jvm.system.utils.getWindowsVersion


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
