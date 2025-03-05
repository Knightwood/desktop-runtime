package androidx.jvm.system

import androidx.jvm.system.core.AppBasePathProvider
import androidx.jvm.system.core.AppInfoProvider
import androidx.jvm.system.utils.SystemProperty
import androidx.jvm.system.utils.noOptionParent
import com.google.auto.service.AutoService
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.nio.file.Paths

@AutoService(AppBasePathProvider::class)
class WindowsAppPathProvider : AppBasePathProvider {
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
