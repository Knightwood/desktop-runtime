package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.savestate.Token
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.github.knightwood.slf4j.kotlin.kLogger
import com.github.knightwood.slf4j.kotlin.logFor
import kotlinx.coroutines.*
import java.util.Collections

interface IFragmentComponentManager {
    fun provideLifeCycle(lifecycleOwner: LifecycleOwner)
    fun <T : Fragment> register(cls: Class<T>, key: Token? = null): Fragment
    fun unregister(key: Token)
    fun <T : Fragment> unregister(component: Fragment)
    fun fragment(key: Token): Fragment
    fun release()

}

inline fun <reified T : Fragment> IFragmentComponentManager.register(key: String? = null): Fragment {
    return register(T::class.java, key)
}

class FragmentManager() : IFragmentComponentManager {
    val logger = logFor("组件管理")
    private lateinit var lifecycleOwner: LifecycleOwner
    private val stackManager: ScreenComponentStackManager = ScreenComponentStackManager()

    override fun provideLifeCycle(lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
    }

    override fun <T : Fragment> register(cls: Class<T>, key: Token?): Fragment {
        val fragment = cls.getDeclaredConstructor().newInstance()
        if (key != null) {
            fragment.token = key
        }
        stackManager.register<T>(fragment)
        fragment.attach(lifecycleOwner)
        lifecycleOwner.lifecycleScope.launch {
            fragment.released.collect {
                if (it) {
                    //不论是手动结束组件，还是因为父组件的生命周期结束而跟随结束，
                    //都已经在内部对资源释放了，这里只需要移除引用，不需要再次调用组件的release方法
//                    logger.info("卸载组件")
                    stackManager.unregister(fragment.token, false)
                }
            }
        }
        return fragment
    }

    override fun unregister(key: Token) {
        stackManager.unregister(key)
    }

    override fun <T : Fragment> unregister(component: Fragment) {
        stackManager.unregister<T>(component)
    }

    override fun fragment(key: Token): Fragment {
        return stackManager.get(key)
    }

    override fun release() {
        stackManager.map.clear()
    }

}


class ScreenComponentStackManager {

    //存储所有已注册的组件
    internal val map = Collections.synchronizedMap<Token, Fragment>(mutableMapOf())

    fun <T : Fragment> register(component: Fragment) {
        if (map.containsKey(component.token)) {
            kLogger.info("${component.token}  已存在")
            return
        }
        map[component.token] = component
    }

    fun <T : Fragment> unregister(component: Fragment, release: Boolean = true) {
        unregister(component.idn, release)
    }

    fun unregister(key: Token, release: Boolean = true) {
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

    fun get(key: Token): Fragment {
        return map[key]!!
    }

}

