package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.savestate.Token
import androidx.lifecycle.Lifecycle
import kotlin.reflect.KClass

/**
 * 使用FragmentProvider创建的Fragment都共享同一个宿主生命周期
 * @param hostLifecycle 宿主生命周期,等同于parentLifecycle.所有创建的Fragment都是用此lifecycle作为parentLifecycle
 */
class FragmentProvider(
    val hostLifecycle: Lifecycle,
) {
    val cache = mutableMapOf<String, Fragment>()

    inline fun <reified T : Fragment> obtain(
        token: Token? = null,
    ): T {
        return obtain(T::class, token)
    }

    /**
     * 获取一个fragment实例,如果实例不存在,则创建它
     * 如果有传入token,则使用此token记录实例,如果为传入token,则使用类的hashcode记录实例.
     * 如果你需要创建一个fragment的多个实例,则必须传入token,否则将获取到同一个实例
     */
    fun <T : Fragment> obtain(cls: KClass<T>, token: Token? = null): T {
        val key = token?.value ?: cls.hashCode().toString()
        return cache.getOrPut(key) {
            val instance =
                if (cls.java.isAssignableFrom(DialogWindowFragment::class.java)) {
                    //如果fragment是DialogWindowFragment的子类,则不传入宿主生命周期
                    fragment<T>(cls, null, token)
                } else {
                    fragment<T>(cls, hostLifecycle, token)
                }
            instance
        } as T
    }

    /**
     * 移除fragment
     * 如果使用obtain获取fragment时传入了token,则使用此方法
     */
    fun remove(token: Token? = null) {
        cache.remove(token?.value)?.also {
            //触发状态保存
            it.onDestroy()
        }
    }

    /**
     * 移除fragment
     * 如果使用obtain获取fragment时未传入token,则使用此方法
     */
    fun remove(cls: KClass<*>) {
        cache.remove(cls.hashCode().toString())?.also {
            //触发状态保存
            it.onDestroy()
        }
    }

}
