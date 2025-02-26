package androidx.compose.desktop.runtime.system.window

import com.sun.jna.platform.win32.WinDef

data class WinAppInfo(val hwnd: WinDef.HWND, val filePath: String) {

    override fun toString(): String {
        return "WinAppInfo(hwnd=$hwnd, filePath='$filePath')"
    }
}
