package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.activity.BundleHolder
import androidx.compose.desktop.runtime.activity.IBundleHolder
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.github.knightwood.slf4j.kotlin.kLogger
import com.github.knightwood.slf4j.kotlin.logFor
import kotlinx.coroutines.*
import java.util.Collections

interface IScreenComponentManager {
    fun provideLifeCycle(lifecycleOwner: LifecycleOwner)
    fun <T : Fragment> register(cls: Class<T>, key: String? = null): Fragment
    fun unregister(key: String)
    fun <T : Fragment> unregister(component: Fragment)
    fun screen(key: String): Fragment
    fun clearScreenComponent()

}

inline fun <reified T : Fragment> IScreenComponentManager.register(key: String? = null): Fragment {
    return register(T::class.java, key)
}

class FragmentManager() : IScreenComponentManager {
    val logger = logFor("组件管理")
    private lateinit var lifecycleOwner: LifecycleOwner
    private val stackManager: ScreenComponentStackManager = ScreenComponentStackManager()
    private val bundleHolder: IBundleHolder by lazy { BundleHolder() }

    override fun provideLifeCycle(lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
    }

    override fun <T : Fragment> register(cls: Class<T>, key: String?): Fragment {
        val fragment = cls.getDeclaredConstructor().newInstance()
        if (key != null) {
            fragment.uuid = key
        }
        stackManager.register<T>(fragment)
        fragment.attach(lifecycleOwner, bundleHolder)
        lifecycleOwner.lifecycleScope.launch {
            fragment.released.collect {
                if (it) {
                    //不论是手动结束组件，还是因为父组件的生命周期结束而跟随结束，
                    //都已经在内部对资源释放了，这里只需要移除引用，不需要再次调用组件的release方法
//                    logger.info("卸载组件")
                    stackManager.unregister(fragment.uuid, false)
                }
            }
        }
        return fragment
    }

    override fun unregister(key: String) {
        stackManager.unregister(key)
    }

    override fun <T : Fragment> unregister(component: Fragment) {
        stackManager.unregister<T>(component)
    }

    override fun screen(key: String): Fragment {
        return stackManager.get(key)
    }

    override fun clearScreenComponent() {
        stackManager.map.clear()
        bundleHolder.clear()
    }

}


class ScreenComponentStackManager {

    //存储所有已注册的组件
    internal val map = Collections.synchronizedMap<String, Fragment>(mutableMapOf())

    fun <T : Fragment> register(component: Fragment) {
        if (map.containsKey(component.uuid)) {
            kLogger.info("${component.uuid}  已存在")
            return
        }
        map[component.uuid] = component
    }

    fun <T : Fragment> unregister(component: Fragment, release: Boolean = true) {
        unregister(component.uuid, release)
    }

    fun unregister(key: String, release: Boolean = true) {
        if (!map.containsKey(key)) {
            return
        }
        map.remove(key)?.also {
            if (release) {
//            logger.info("ScreenComponent $key release")
                it.release()
            }
        }
    }

    fun get(key: String): Fragment {
        return map[key]!!
    }

}

