package com.github.knightwood.example.path

import androidx.jvm.system.core.AppPathProvider
import androidx.jvm.system.core.keepDirExist
import androidx.jvm.system.core.keepFileExist
import okio.Path

/**
 * 数据库、datastore、log文件、数据文件（生成的证书、记录的excel文件等）
 */
object FilePathProvider {

    /**
     * 配置文件、数据库、datastore等都放在此目录下
     */
    val internalConfigDir: Path
        get() {
            return AppPathProvider.provider.internalConfigDirPath.keepDirExist()
        }

    /**
     * 公共配置目录
     */
    val publicConfigDir: Path
        get() {
            return AppPathProvider.provider.configDirPath.keepDirExist()
        }

    /**
     * 数据库文件
     */
    val dbFile: Path
        get() = internalConfigDir.resolve("ii1.db")

    /**
     * datastore文件
     */
    val dataStoreFile: Path
        get() = internalConfigDir.resolve("ii1.preferences_pb")

    /**
     * 日志文件夹 已在logback.xml中指定，在代码中配置太麻烦了。 这里的路径仅用于操作日志文件，需要注意与logback中对应。
     */
    val logDir: Path
        get() {
            return publicConfigDir.resolve("log").keepDirExist()
        }

    /**
     * 用户的各类文件存放地址
     */
    val dataDir: Path
        get() {
            return publicConfigDir.resolve("data").keepDirExist()
        }

    /**
     * 插件存放目录
     */
    val pluginsDir: Path
        get() {
            return publicConfigDir.resolve("plugins").keepDirExist()
        }

    /**
     * 插件存放目录
     */
    val internalPluginsDir: Path
        get() {
            return internalConfigDir.resolve("plugins").keepDirExist()
        }

    /**
     * 使用进程锁的lock文件
     */
    val lockFilePath: Path
        get() {
            return internalConfigDir.resolve("app_lock.lz").keepFileExist()
        }
}
