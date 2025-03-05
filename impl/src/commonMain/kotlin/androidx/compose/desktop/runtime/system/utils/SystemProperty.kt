package androidx.compose.desktop.runtime.system.utils

object SystemProperty {

    operator fun get(key: String): String? = System.getProperty(key)

    fun get(
        key: String,
        default: String,
    ): String = System.getProperty(key, default)

    operator fun set(
        key: String,
        value: String,
    ) {
        System.setProperty(key, value)
    }

    //<editor-fold desc="快速方法">
    fun os(): String {
        return get("os.name")!!
    }

    fun arch(): String {
        return get("os.arch")!!
    }

    fun javaVer(): String {
        val javaOsVersion = get("os.version")
        return javaOsVersion!!
    }
    //</editor-fold>
}
