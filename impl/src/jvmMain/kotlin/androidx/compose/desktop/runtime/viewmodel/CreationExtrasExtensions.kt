package androidx.compose.desktop.runtime.viewmodel

import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras

/**
 * 从ViewModelStoreOwner提供的默认CreationExtras中创建一个新的CreationExtras实例
 */
fun ViewModelStoreOwner.mutableCreationExtrasOf(builder: MutableCreationExtras.() -> Unit): CreationExtras {
    val defaultCreationExtras = ViewModelProviders.getDefaultCreationExtras(this)
    val result = MutableCreationExtras(defaultCreationExtras).apply(builder)
    return result
}

fun CreationExtras.toMutable(extrasBuilder: MutableCreationExtras.() -> Unit): MutableCreationExtras {
    val result = MutableCreationExtras(this).apply(extrasBuilder)
    return result
}
