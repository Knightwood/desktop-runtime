package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.activity.BundleHolder
import androidx.lifecycle.LifecycleOwner
import com.github.knightwood.slf4j.kotlin.logger
import java.util.Collections

interface IScreenComponentManager {
    fun provideLifeCycle(lifecycleOwner: LifecycleOwner)
    fun <T : ScreenComponent> register(cls: Class<T>, key: String? = null): ScreenComponent
    fun unregister(key: String)
    fun <T : ScreenComponent> unregister(component: ScreenComponent)
    fun screen(key: String): ScreenComponent
    fun clearScreenComponent()

}

inline fun <reified T : ScreenComponent> IScreenComponentManager.register(key: String? = null): ScreenComponent {
    return register(T::class.java, key)
}

class ScreenComponentManager() : IScreenComponentManager {
    lateinit var lifecycleOwner: LifecycleOwner
    private val screenComponentStackManager: ScreenComponentStackManager = ScreenComponentStackManager()
    private val componentBundle: BundleHolder by lazy { BundleHolder() }

    override fun provideLifeCycle(lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
    }

    override fun <T : ScreenComponent> register(cls: Class<T>, key: String?): ScreenComponent {
        val fragment = cls.getDeclaredConstructor().newInstance()
        if (key != null) {
            fragment.uuid = key
        }
        screenComponentStackManager.register<T>(fragment)
        fragment.attach(lifecycleOwner.lifecycle, componentBundle)
        return fragment
    }

    override fun unregister(key: String) {
        screenComponentStackManager.unregister(key)
    }

    override fun <T : ScreenComponent> unregister(component: ScreenComponent) {
        screenComponentStackManager.unregister<T>(component)
    }

    override fun screen(key: String): ScreenComponent {
        return screenComponentStackManager.get(key)
    }

    override fun clearScreenComponent() {
        screenComponentStackManager.map.clear()
        componentBundle.bundleSaverMap.clear()
    }

}


class ScreenComponentStackManager {
    //存储所有已注册的组件
    internal val map = Collections.synchronizedMap<String, ScreenComponent>(mutableMapOf())

    fun <T : ScreenComponent> register(component: ScreenComponent) {
        if (map.containsKey(component.uuid)) {
            logger.info("${component.uuid}  已存在")
            return
        }
        map[component.uuid] = component
    }

    fun <T : ScreenComponent> unregister(component: ScreenComponent) {
        unregister(component.uuid)
    }

    fun unregister(key: String) {
        if (!map.containsKey(key)) {
            return
        }
        map.remove(key)?.also {
//            logger.info("ScreenComponent $key release")
            it.release()
        }
    }

    fun get(key: String): ScreenComponent {
        return map[key]!!
    }

}

