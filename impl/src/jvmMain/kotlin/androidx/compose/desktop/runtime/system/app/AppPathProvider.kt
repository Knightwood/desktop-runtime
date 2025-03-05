package androidx.compose.desktop.runtime.system.app

import androidx.compose.desktop.runtime.system.utils.*
import androidx.compose.desktop.runtime.system.utils.SystemOs.*
import com.github.knightwood.slf4j.kotlin.info
import com.github.knightwood.slf4j.kotlin.logger
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.nio.file.Files
import java.nio.file.Paths


/**
 * 此接口提供软件的一些路径信息 例如：
 *
 * ```
 * userHome: C:\Users\junya,
 * pasteAppPath: C:\Users\junya\AppData\Local\multi-device controller\app,
 * pasteAppJarPath: C:\Users\junya\AppData\Local\multi-device controller\app\resources,
 * pasteAppExePath: C:\Users\junya\AppData\Local\multi-device controller\app\bin,
 * configPath: C:\Users\junya\.测试
 * ```
 */
interface AppBasePathProvider {
    /**
     * 用户目录
     */
    val userHome: Path

    /**
     * 软件的安装目录
     */
    val installPath: Path

    /**
     * 软件安装后的jar包路径
     */
    val installedJarPath: Path

    /**
     * 软件安装后的exe文件路径
     */
    val installedExePath: Path

    /**
     * 配置文件路径，通常按照linux样式,位于 “/用户目录/应用名称”
     */
    val configDirPath: Path
}

/**
 * 提供应用的路径信息，比如应用安装目录，系统的用户目录等 提供如下目录：用户目录路径，软件安装路径，默认配置文件夹路径。 提供路径注册功能
 */
object AppPathProvider : AppBasePathProvider by SystemPath() {

    fun print() {
        logger.info {
            val userHome = AppPathProvider.userHome
            val installPath = AppPathProvider.installPath
            val installedJarPath = AppPathProvider.installedJarPath
            val installedExePath = AppPathProvider.installedExePath
            val configPath = AppPathProvider.configDirPath
            " 用户目录: $userHome\n 安装路径: $installPath\n 程序jar包路径: $installedJarPath\n exe文件路径: $installedExePath\n 配置目录路径: $configPath\n"
        }
    }
}

private fun SystemPath(): AppBasePathProvider {
    return if (AppInfoProvider.get().isDevMode)
        DevelopmentAppPathProvider()
    else
        when (currentOS) {
            MacOS -> MacosAppPathProvider()
            Windows -> WindowsAppPathProvider()
            Linux -> LinuxAppPathProvider()
        }
}

class DevelopmentAppPathProvider : AppBasePathProvider {

    private val composeAppDir = SystemProperty.get("user.dir")!!

    override val userHome: Path = Paths.get(SystemProperty.get("user.home")!!).toOkioPath()

    override val installPath: Path = composeAppDir.toPath()

    override val installedJarPath: Path = getResources()

    override val installedExePath: Path = getResources()

    override val configDirPath: Path = getUserPath()

    private fun getUserPath(): Path {
        return composeAppDir.toPath().resolve(".config")
    }

    private fun getResources(): Path {
        val resources = composeAppDir.toPath().resolve("resources")
        return resources.resolve(currentOsAndArch)
    }
}

private class WindowsAppPathProvider : AppBasePathProvider {
    override val userHome: Path
        get() = Paths.get(SystemProperty["user.home"]!!).toOkioPath()

    override val installPath: Path = getAppJarPath().noOptionParent.normalized()

    override val installedJarPath: Path = getAppJarPath()

    override val installedExePath: Path = getAppExePath()

    override val configDirPath: Path = getUserPath()

    private fun getAppJarPath(): Path {
        SystemProperty.get("compose.application.resources.dir")?.let {
            return it.toPath()
        }
        SystemProperty.get("skiko.library.path")?.let {
            return it.toPath()
        }
        throw IllegalStateException("Could not find app path")
    }

    private fun getAppExePath(): Path {
        return getAppJarPath().noOptionParent.resolve("bin").normalized()
    }

    private fun getUserPath(): Path {
        return userHome.resolve(".${AppInfoProvider.get().appName}")
    }
}

private class LinuxAppPathProvider : AppBasePathProvider {
    override val userHome: Path = Paths.get(SystemProperty["user.home"]!!).toOkioPath()

    override val installPath: Path = getAppJarPath().noOptionParent.noOptionParent.normalized()

    override val installedJarPath: Path = getAppJarPath()

    override val installedExePath: Path = getAppExePath()

    override val configDirPath: Path = getUserPath()

    private fun getAppJarPath(): Path {
        SystemProperty.get("compose.application.resources.dir")?.let {
            return it.toPath()
        }
        SystemProperty.get("skiko.library.path")?.let {
            return it.toPath()
        }
        throw IllegalStateException("Could not find app path")
    }

    private fun getAppExePath(): Path {
        return getAppJarPath().noOptionParent.resolve("runtime").resolve("lib").normalized()
    }

    private fun getUserPath(): Path {
        return userHome.resolve(".local").resolve("shard").resolve(".${AppInfoProvider.get().appName}")
    }
}

/**
 * ```
 * .
 * ├── Info.plist
 * ├── MacOS
 * ├── PkgInfo
 * ├── Resources
 * ├── _CodeSignature
 * ├── app
 * └── runtime
 * ```
 */
private class MacosAppPathProvider : AppBasePathProvider {

    override val userHome: Path = Paths.get(SystemProperty["user.home"]!!).toOkioPath()

    override val installPath: Path = getAppJarPath().noOptionParent.noOptionParent.normalized()

    override val installedJarPath: Path = getAppJarPath()

    override val installedExePath: Path = getAppExePath()

    override val configDirPath: Path = getUserPath()

    private fun getAppJarPath(): Path {

        SystemProperty.get("compose.application.resources.dir")?.let {
            return it.toPath()
        }
        SystemProperty.get("skiko.library.path")?.let {
            return it.toPath()
        }
        throw IllegalStateException("Could not find app path")
    }

    private fun getAppExePath(): Path {
        return getAppJarPath().noOptionParent
            .resolve("runtime")
            .resolve("Contents")
            .resolve("Home")
            .resolve("lib")
            .normalized()
    }

    private fun getUserPath(): Path {
        val appSupportPath =
            userHome.resolve("Library")
                .resolve("Application Support")
                .resolve(AppInfoProvider.get().appName)
        val appSupportNioPath = appSupportPath.toNioPath()
        if (Files.notExists(appSupportNioPath)) {
            Files.createDirectories(appSupportNioPath)
        }

        return appSupportPath
    }
}

