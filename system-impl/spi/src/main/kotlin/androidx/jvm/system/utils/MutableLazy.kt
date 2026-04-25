package androidx.jvm.system.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class MutableLazy<O : Any>(val initializer: () -> O) : ReadWriteProperty<Any?, O> {
    var instance: O? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): O {
        return instance ?: synchronized(this) {
            instance ?: initializer().also { instance = it }
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: O) {
        instance = value
    }

}
