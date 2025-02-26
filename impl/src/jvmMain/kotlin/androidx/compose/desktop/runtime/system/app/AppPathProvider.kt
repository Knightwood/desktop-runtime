package androidx.compose.desktop.runtime.system.app

import androidx.compose.desktop.runtime.system.utils.SystemOs.*
import androidx.compose.desktop.runtime.system.utils.SystemProperty
import androidx.compose.desktop.runtime.system.utils.currentOS
import androidx.compose.desktop.runtime.system.utils.noOptionParent
import com.github.knightwood.slf4j.kotlin.info
import com.github.knightwood.slf4j.kotlin.logger
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.nio.file.Paths

/**
 * 此接口提供软件的一些路径信息
 * 例如：
 * ```
 * userHome: C:\Users\junya,
 * pasteAppPath: C:\Users\junya\AppData\Local\multi-device controller\app,
 * pasteAppJarPath: C:\Users\junya\AppData\Local\multi-device controller\app\resources,
 * pasteAppExePath: C:\Users\junya\AppData\Local\multi-device controller\app\bin,
 * configPath: C:\Users\junya\.测试
 * ```
 */
interface IAppPath {
    /**
     * 用户目录
     */
    val userHome: Path

    /**
     * 软件的安装目录
     */
    val pasteAppPath: Path

    /**
     * 软件安装后的jar包路径
     */
    val pasteAppJarPath: Path

    /**
     * 软件安装后的exe文件路径
     */
    val pasteAppExePath: Path

    /**
     * 配置文件路径，通常按照linux样式,位于 “/用户目录/应用名称”
     */
    val configDirPath: Path
}

/**
 * 提供应用的路径信息，比如应用安装目录，系统的用户目录等
 */
object AppPathProvider : IAppPath by parsePath() {

    fun print(){
        logger.info {
            val userHome = AppPathProvider.userHome
            val pasteAppPath = AppPathProvider.pasteAppPath
            val pasteAppJarPath = AppPathProvider.pasteAppJarPath
            val pasteAppExePath = AppPathProvider.pasteAppExePath
            val configPath = AppPathProvider.configDirPath
            " userHome: $userHome\n pasteAppPath: $pasteAppPath\n pasteAppJarPath: $pasteAppJarPath\n pasteAppExePath: $pasteAppExePath\n configPath: $configPath\n"
        }
    }
}

fun parsePath(): IAppPath {
    return when (currentOS) {
        MacOS -> TODO()
        Windows -> WinAppPath()
        Linux -> TODO()
    }
}

private class WinAppPath : IAppPath {
    override val userHome: Path
        get() = Paths.get(SystemProperty["user.home"]!!).toOkioPath()

    override val pasteAppPath: Path = getAppJarPath().noOptionParent.normalized()

    override val pasteAppJarPath: Path = getAppJarPath()

    override val pasteAppExePath: Path = getAppExePath()

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
        return userHome.resolve(".${AppInfoHolder.get().appName}")
    }
}
