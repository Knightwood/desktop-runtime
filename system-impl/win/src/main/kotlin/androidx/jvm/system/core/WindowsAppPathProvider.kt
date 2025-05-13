package androidx.jvm.system.core

import androidx.jvm.system.utils.SystemProperty
import androidx.jvm.system.utils.noOptionParent
import com.google.auto.service.AutoService
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.nio.file.Paths

/**
 * /应用名称                ---应用目录顶层       a
 *      /app               ---存储所有的jar包     b
 *      /app.exe           ---exe文件，应用入口   c
 *      /runtime           ---java运行时         d
 *      /conf              ---配置文件           e
 *
 */
@AutoService(AppBasePathProvider::class)
class WindowsAppPathProvider : AppBasePathProvider {
    /**
     * 系统用户目录
     */
    override val userHome: Path = SystemProperty["user.home"]!!.toPath()

    /**
     * 安装路径，也就是a路径
     */
    override val installPath: Path = PathService.getAppJarPath().noOptionParent
    //SystemProperty["user.dir"]!!.toPath()

    /**
     * jar包路径，就是b路径
     */
    override val installedJarPath: Path = PathService.getAppJarPath()
    //installPath.resolve("app")

    /**
     * exe路径，就是c路径
     */
    override val installedExePath: Path = installPath

    /**
     * 外部配置文件存储目录
     */
    override val configDirPath: Path get() = userHome.resolve(".${AppInfoProvider.get().appName}")

    /**
     * 配置文件存储目录，就是e路径
     */
    override val internalConfigDirPath: Path = installPath.resolve(".conf")

}
