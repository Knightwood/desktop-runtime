package androidx.jvm.system.core

import java.util.*


fun getAutoStartService(): AppStartUpService? {
    return ServiceLoader.load(AppStartUpService::class.java).firstOrNull()
}

interface AppStartUpService {
    fun followConfig()               // 根据配置设置是否开机启动
    fun isAutoStartUp(): Boolean     // 检查是否已启用开机启动
    fun makeAutoStartUp()            // 启用开机启动
    fun removeAutoStartUp()          // 禁用开机启动
}
