package androidx.compose.desktop.runtime.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class WeakReferenceReadableDelegate<T>(ref: T?) : ReadWriteProperty<Any?, T?> {
    private val javaReference: java.lang.ref.WeakReference<T> =
        java.lang.ref.WeakReference(ref)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return javaReference.get()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        if (value == null) {
            javaReference.clear()
        } else {
            throw java.lang.IllegalStateException("${property.name} cannot be set other value.")
        }
    }
}

class WeakReferenceDelegate<T> : ReadWriteProperty<Any?, T?> {
    private lateinit var javaReference: java.lang.ref.WeakReference<T>
    val isInitialized: Boolean get() = this::javaReference.isInitialized

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        if (!isInitialized) {
            return null
        }
        return javaReference.get()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        if (value == null) {
            if (isInitialized) {
                javaReference.clear()
            }
        } else {
            if (isInitialized) {
                throw IllegalStateException("Already initialized")
            }
            this.javaReference = java.lang.ref.WeakReference(value)
        }
    }
}
