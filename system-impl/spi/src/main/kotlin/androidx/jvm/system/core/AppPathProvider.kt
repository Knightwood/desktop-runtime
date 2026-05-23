package androidx.jvm.system.core

import androidx.jvm.system.di.getPlatformInstance
import androidx.jvm.system.process.ProcessInfoHelper
import androidx.jvm.system.utils.JvmUtils
import androidx.jvm.system.utils.MutableLazy
import androidx.jvm.system.utils.SystemProperty
import androidx.jvm.system.utils.noOptionParent
import com.github.knightwood.slf4j.kotlin.info
import com.github.knightwood.slf4j.kotlin.kLogger
import okio.FileNotFoundException
import okio.Path as OkioPath
import okio.Path.Companion.toPath as toOkioPath
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
    val userHome: OkioPath

    /**
     * 软件的安装目录
     */
    val installPath: OkioPath

    /**
     * 软件安装后的jar包路径
     */
    val installedJarPath: OkioPath

    /**
     * 软件安装后的exe文件路径
     */
    val installedExePath: OkioPath

    /**
     * 配置文件路径，通常按照linux样式,位于 “/用户目录/应用名称”
     */
    val configDirPath: OkioPath

    /**
     * 配置文件路径，位于程序安装目录的conf文件夹下
     */
    val internalConfigDirPath: OkioPath

    fun print() {}
}

/**
 * 判断文件夹是否存在，如果不存在，则创建文件夹
 */
fun OkioPath.keepDirExist(): OkioPath {
    val dir = this.toFile()
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return this
}

/**
 * 判断文件是否存在，如果不存在，则创建文件
 */
fun OkioPath.keepFileExist(): OkioPath {
    val dir = this.toFile()
    if (!dir.exists()) {
        dir.createNewFile()
    }
    return this
}

fun AppPathProvider.resourcePath(): OkioPath {
    SystemProperty.get("compose.application.resources.dir")?.let {
        return it.toOkioPath()
    } ?: throw FileNotFoundException("compose.application.resources.dir not found")
}

fun AppPathProvider.skikoPath(): OkioPath {
    SystemProperty.get("skiko.library.path")?.let {
        return it.toOkioPath()
    } ?: throw FileNotFoundException("compose.application.resources.dir not found")
}

fun AppPathProvider.jvmPath(): OkioPath {
    SystemProperty.get("java.home")?.let {
        return it.toOkioPath()
    } ?: throw FileNotFoundException("jvm.library.path not found")
}

/**
 * 提供应用的路径信息，比如应用安装目录，系统的用户目录等 提供如下目录：用户目录路径，软件安装路径，默认配置文件夹路径。 提供路径注册功能
 */
class AppPathProvider private constructor(
    private val pathProviderImpl: AppBasePathProvider,
) : AppBasePathProvider by pathProviderImpl {

    override fun print() {
        kLogger.info {
            val appPathProvider = this
            val userHome = appPathProvider.userHome
            val installPath = appPathProvider.installPath
            val installedJarPath = appPathProvider.installedJarPath
            val installedExePath = appPathProvider.installedExePath
            val configPath = appPathProvider.configDirPath
            "\n 用户目录: $userHome\n 安装路径: $installPath\n 程序jar包路径: $installedJarPath\n exe文件路径: $installedExePath\n 配置目录路径: $configPath\n 内部配置目录路径: $internalConfigDirPath\n"
        }
    }

    companion object {
        @Volatile
        internal var appPathProvider: AppPathProvider? = null

        fun getInstance(impl: AppBasePathProvider = ProviderImpl): AppBasePathProvider {
            return appPathProvider ?: synchronized(this) {
                appPathProvider ?: AppPathProvider(impl).also { appPathProvider = it }
            }
        }

        /**
         * 使用属性获取实例
         */
        val provider get() = getInstance()
    }
}

internal val ProviderImpl by lazy {
    //需要判断是否在ide环境
    val path = PathService.getAppJarDirPath().toFile().absolutePath
    val regex = Regex("build[\\\\/](libs|classes)") // 匹配 "build/libs" 或 "build/classes"
    val matches = regex.containsMatchIn(path)
    val isProcessNameIsJava = ProcessInfoHelper.sampleInfo.isProcessNameIsJava
    if (matches || isProcessNameIsJava) {
        DebugAppPathProvider()
    } else {
        getPlatformInstance<AppBasePathProvider>()
//        requireNotNull(
//            ServiceLoader
//                .load(AppBasePathProvider::class.java)
//                .firstOrNull()
//        ) {
//            "Implementation for AppBasePathProvider not found"
//        }
    }
}


object PathService {
    /**
     * 用于获取应用程序路径
     */
    var anyClass: Class<*> by MutableLazy { ProcessInfoHelper.sampleInfo.mainClass }

    /**
     * jar包所在目录路径
     */
    private var cache: MutableMap<Class<*>, OkioPath> = mutableMapOf()

    /**
     * 获取jar包的parent路径
     */
    fun getAppJarDirPath(): OkioPath {
        checkNotNull(anyClass) {
            "anyClass is null"
        }
        return getAppJarDirPath(anyClass)
    }

    /**
     * 不推荐使用System.getProperty.get("user.dir")获取jar包路径
     * 原因：如果程序是有系统服务、计划任务之类的启动，获取的路径有可能是 C:\Windows\System32
     *
     * 为什么必须传入一个class，而不是默认一个：
     * 在idea中运行时，比如引入的jar包是来自本地.m2仓库，那么获取的路径是.m2仓库下的jar包路径，而不是项目目录
     */
    fun getAppJarDirPath(clazz: Class<*>): OkioPath {
        return cache[clazz] ?: JvmUtils.getJarPath(clazz).toOkioPath().noOptionParent.also {
            cache[clazz] = it
        }
    }

}

class DebugAppPathProvider : AppBasePathProvider {
    private val composeAppDir = SystemProperty.get("user.dir")!!.toOkioPath()

    /**
     * 安装路径：project路径
     */
    override val installPath: OkioPath = composeAppDir

    /**
     * jar包的存储路径，ide环境下不存在
     */
    override val installedJarPath: OkioPath = "".toOkioPath()

    /**
     * exe路径不存在，ide环境下不存在
     */
    override val installedExePath: OkioPath = "".toOkioPath()

    /**
     * 系统用户目录
     */
    override val userHome: OkioPath = SystemProperty["user.home"]!!.toOkioPath()

    override val configDirPath: OkioPath
        get() = userHome.resolve(".${AppInfoProvider.get().appName}")

    /**
     * 配置文件路径，位于project目录的.conf文件夹下
     */
    override val internalConfigDirPath: OkioPath = composeAppDir.resolve(".conf")

}



