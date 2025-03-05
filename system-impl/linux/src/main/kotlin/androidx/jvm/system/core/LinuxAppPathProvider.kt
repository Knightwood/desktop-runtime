package androidx.jvm.system.core
import androidx.jvm.system.utils.SystemProperty
import androidx.jvm.system.utils.noOptionParent
import com.google.auto.service.AutoService
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.nio.file.Paths

@AutoService(AppBasePathProvider::class)
class LinuxAppPathProvider : AppBasePathProvider {
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
