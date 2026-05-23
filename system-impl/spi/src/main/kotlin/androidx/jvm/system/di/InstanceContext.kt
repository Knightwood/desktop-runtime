package androidx.jvm.system.di

import androidx.jvm.system.utils.currentOS
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.context.KoinContext
import org.koin.core.error.KoinApplicationAlreadyStartedException
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import java.util.ServiceLoader

object InstanceContext : KoinContext {

    private var _koin: Koin? = null
    private var _koinApplication: KoinApplication? = null

    override fun get(): Koin = _koin ?: error("KoinApplication has not been started")

    override fun getOrNull(): Koin? = _koin

    fun getKoinApplicationOrNull(): KoinApplication? = _koinApplication

    private fun register(koinApplication: KoinApplication) {
        if (_koin != null) {
            throw KoinApplicationAlreadyStartedException("A Koin Application has already been started")
        }
        _koinApplication = koinApplication
        _koin = koinApplication.koin
    }

    override fun stopKoin() = synchronized(this) {
        _koin?.close()
        _koin = null
    }

    override fun startKoin(koinApplication: KoinApplication): KoinApplication = synchronized(this) {
        register(koinApplication)
        koinApplication.createEagerInstances()
        koinApplication.modules(findModules())//加载其他平台注册进来的模块
        return koinApplication
    }

    private fun findModules(): MutableList<Module> {
        val modules = mutableListOf<Module>()
        val providers = ServiceLoader.load(ModuleProvider::class.java)
        providers.forEach {
            modules.add(it.provide())
        }
        return modules
    }

    override fun startKoin(appDeclaration: KoinAppDeclaration): KoinApplication = synchronized(this) {
        val koinApplication = KoinApplication.init()
        appDeclaration(koinApplication)
        return startKoin(koinApplication)
    }

    override fun loadKoinModules(module: Module, createEagerInstances: Boolean) = synchronized(this) {
        get().loadModules(listOf(module), createEagerInstances = createEagerInstances)
    }

    override fun loadKoinModules(modules: List<Module>, createEagerInstances: Boolean) = synchronized(this) {
        get().loadModules(modules, createEagerInstances = createEagerInstances)
    }

    override fun unloadKoinModules(module: Module) = synchronized(this) {
        get().unloadModules(listOf(module))
    }

    override fun unloadKoinModules(modules: List<Module>) = synchronized(this) {
        get().unloadModules(modules)
    }
}

/**
 * 使一个类实现此接口，调用by inject时将从InstanceContext持有的koin中获取实例
 * 而非从全局koin中获取实例
 */
interface InstanceKoinComponent : KoinComponent {
    override fun getKoin(): Koin = InstanceContext.get()
}

/**
 * app模块调用此方法启动koin、加载其他平台注册的模块
 * 并加载通过参数传入的模块
 */
fun InstanceContext.startUp(appDeclaration: KoinAppDeclaration = {}) {
    startKoin(appDeclaration = appDeclaration)
}

/**
 * 获取当前平台下某个类的实例
 */
inline fun <reified T : Any> getPlatformInstance(): T {
    return InstanceContext.get().get<T>(qualifier = named(currentOS))
}
