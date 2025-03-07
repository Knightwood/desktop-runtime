package androidx.jvm.system.core

import androidx.jvm.system.utils.SystemProperty
import androidx.jvm.system.utils.currentOsAndArch
import com.github.knightwood.slf4j.kotlin.info
import com.github.knightwood.slf4j.kotlin.logger
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.nio.file.Paths
import java.util.*


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

    fun print() {}
}

/**
 * 提供应用的路径信息，比如应用安装目录，系统的用户目录等 提供如下目录：用户目录路径，软件安装路径，默认配置文件夹路径。 提供路径注册功能
 */
class AppPathProvider private constructor(
    private val pathProviderImpl: AppBasePathProvider
) : AppBasePathProvider by pathProviderImpl {

    override fun print() {
        logger.info {
            val appPathProvider = this
            val userHome = appPathProvider.userHome
            val installPath = appPathProvider.installPath
            val installedJarPath = appPathProvider.installedJarPath
            val installedExePath = appPathProvider.installedExePath
            val configPath = appPathProvider.configDirPath
            " 用户目录: $userHome\n 安装路径: $installPath\n 程序jar包路径: $installedJarPath\n exe文件路径: $installedExePath\n 配置目录路径: $configPath\n"
        }
    }

    companion object {
        @Volatile
        internal var appPathProvider: AppPathProvider? = null

        fun getInstance(impl: AppBasePathProvider = SystemPath()): AppBasePathProvider {
            return appPathProvider ?: synchronized(this) {
                appPathProvider ?: AppPathProvider(impl).also { appPathProvider = it }
            }
        }

        /**
         * 使用属性获取实例
         */
        val AppPathProvider get() = getInstance()
    }
}

private fun SystemPath(): AppBasePathProvider {
    return if (AppInfoProvider.get().isDevMode)
        DevelopmentAppPathProvider()
    else
        requireNotNull(
            ServiceLoader
                .load(AppBasePathProvider::class.java)
                .firstOrNull()
        ) {
            "Implementation for AppBasePathProvider not found"
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




