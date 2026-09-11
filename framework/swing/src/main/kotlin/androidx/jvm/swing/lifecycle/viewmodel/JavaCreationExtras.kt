package androidx.jvm.swing.lifecycle.viewmodel

import androidx.compose.desktop.runtime.viewmodel.mutableCreationExtrasOf
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras


/**
 * 辅助在java中获取和修改CreationExtras
 */
object JavaCreationExtras {
    val EmptyCreationExtras get() = CreationExtras.Empty

    @JvmStatic
    fun mutableCreationExtrasOf(
        owner: ViewModelStoreOwner,
        builder: MutableCreationExtrasBuilder,
    ): CreationExtras {
        return owner.mutableCreationExtrasOf {
            builder.doActionFor(this)
        }
    }

    @JvmStatic
    fun mutableCreationExtrasOf(
        builder: MutableCreationExtrasBuilder,
    ): CreationExtras {
        return MutableCreationExtras().apply {
            builder.doActionFor(this)
        }
    }

    /**
     * 修改已有的CreationExtras
     */
    fun modify(exist: CreationExtras, creationExtrasBuilder: MutableCreationExtrasBuilder): MutableCreationExtras {
        return exist.modify{
            creationExtrasBuilder.doActionFor(this)
        }
    }
}

fun interface MutableCreationExtrasBuilder {
    fun doActionFor(mutableCreationExtras: MutableCreationExtras)
}

/**
 * 修改已有的CreationExtras
 */
fun CreationExtras.modify(action: MutableCreationExtras.() -> Unit): MutableCreationExtras {
    return MutableCreationExtras(this).apply(action)
}
