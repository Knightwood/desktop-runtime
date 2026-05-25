package androidx.jvm.system.windows

/**
 * 一些不同平台通用的窗口操作功能
 */
interface PlatformWindowHelper {
    fun bringToFront(desc: WindowDesc) {}

    fun bringToBack(desc: WindowDesc) {}

    fun close(desc: WindowDesc) {}
}
