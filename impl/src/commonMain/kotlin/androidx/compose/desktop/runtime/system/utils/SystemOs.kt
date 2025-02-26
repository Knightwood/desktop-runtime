package androidx.compose.desktop.runtime.system.utils

enum class SystemOs {
    MacOS, Windows, Linux,
}

enum class Arch {
    X64, Arm64, X32,
}

val currentOS: SystemOs by lazy {
    val os = System.getProperty("os.name")
    when {
        os.equals("Mac OS X", ignoreCase = true)
                || os.contains("mac")
                || os.contains("darwin") -> SystemOs.MacOS

        os.startsWith("Win", ignoreCase = true) -> SystemOs.Windows
        os.startsWith("Linux", ignoreCase = true) -> SystemOs.Linux
        else -> error("Unknown OS name: $os")
    }
}

val currentArch by lazy {
    val osArch = System.getProperty("os.arch")
    when (osArch) {
        "x86_64", "amd64" -> Arch.X64
        "aarch64" -> Arch.Arm64
        "x86" -> Arch.X32
        else -> error("Unsupported OS arch: $osArch")
    }
}

val is64Arch by lazy {
    currentArch == Arch.X64 || currentArch == Arch.Arm64
}


fun getWindowsVersion(
    osName: String,
    javaOsVersion: String,
): String {
    val parts = osName.split(" ", limit = 2)
    return if (parts.size > 1) parts[1] else javaOsVersion
}
