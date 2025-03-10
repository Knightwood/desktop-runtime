@file:Suppress("UNCHECKED_CAST")

package androidx.compose.desktop.runtime.domain

import androidx.compose.desktop.runtime.context.IContext
import androidx.compose.desktop.runtime.context.LocalContext
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.core.bundle.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import java.io.Serializable

/**
 * 功能类似android中调用setContent时，其内部提供LocalSaveableStateRegistry。
 */
@Composable
fun ProvideAndroidCompositionLocals(
    id: String,
    context: IContext,
    lifecycleOwner: LifecycleOwner,
    viewModelStoreOwner: ViewModelStoreOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    content: @Composable () -> Unit
) {
    val saveableStateRegistry = remember {
        DisposableSaveableStateRegistry(id, savedStateRegistryOwner)
    }
    DisposableEffect(Unit) {
        onDispose {
            saveableStateRegistry.dispose()
        }
    }
    CompositionLocalProvider(
        LocalContext provides context,
        LocalLifecycleOwner provides lifecycleOwner,
        LocalViewModelStoreOwner provides viewModelStoreOwner,
        LocalSaveableStateRegistry provides saveableStateRegistry,
    ) {
        content()
    }
}

/**
 * Creates [DisposableSaveableStateRegistry] with the restored values using
 * [SavedStateRegistry] and saves the values when [SavedStateRegistry]
 * performs save.
 *
 * To provide a namespace we require unique [id]. We can't use the default
 * way of doing it when we have unique id on [AbstractComposeView]
 * because we dynamically create [AbstractComposeView]s and there is
 * no way to have a unique id given there are could be any number
 * of [AbstractComposeView]s inside the same Activity. If we use
 * [View.generateViewId] this id will not survive Activity recreation.
 * But it is reasonable to ask our users to have an unique id on
 * [AbstractComposeView].
 */
internal fun DisposableSaveableStateRegistry(
    id: String,
    savedStateRegistryOwner: SavedStateRegistryOwner
): DisposableSaveableStateRegistry {
    val key = "${SaveableStateRegistry::class.java.simpleName}:$id"

    //获取从activity中的savedStateRegistry
    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    //使用key得到之前保存的数据
    val bundle = androidxRegistry.consumeRestoredStateForKey(key)
    val restored: Map<String, List<Any?>>? = bundle?.toMap()

    val saveableStateRegistry = SaveableStateRegistry(restoredValues = restored, canBeSaved = ::canBeSavedToBundle)
    val registered = try {
        androidxRegistry.registerSavedStateProvider(key) {
            saveableStateRegistry.performSave().toBundle()
        }
        true
    } catch (ignore: IllegalArgumentException) {
        // this means there are two AndroidComposeViews composed into different parents with the
        // same view id. currently we will just not save/restore state for the second
        // AndroidComposeView.
        // TODO: we should verify our strategy for such cases and improve it. b/162397322
        false
    }
    return DisposableSaveableStateRegistry(saveableStateRegistry) {
        if (registered) {
            androidxRegistry.unregisterSavedStateProvider(key)
        }
    }
}

/**
 * [SaveableStateRegistry] which can be disposed using [dispose].
 */
internal class DisposableSaveableStateRegistry(
    saveableStateRegistry: SaveableStateRegistry,
    private val onDispose: () -> Unit
) : SaveableStateRegistry by saveableStateRegistry {

    fun dispose() {
        onDispose()
    }
}

/**
 * Checks that [value] can be stored inside [Bundle].
 */
private fun canBeSavedToBundle(value: Any): Boolean {
    // SnapshotMutableStateImpl is Parcelable, but we do extra checks
    if (value is SnapshotMutableState<*>) {
        if (value.policy === neverEqualPolicy<Any?>() ||
            value.policy === structuralEqualityPolicy<Any?>() ||
            value.policy === referentialEqualityPolicy<Any?>()
        ) {
            val stateValue = value.value
            return if (stateValue == null) true else canBeSavedToBundle(stateValue)
        } else {
            return false
        }
    }
    // lambdas in Kotlin implement Serializable, but will crash if you really try to save them.
    // we check for both Function and Serializable (see kotlin.jvm.internal.Lambda) to support
    // custom user defined classes implementing Function interface.
    if (value is Function<*> && value is Serializable) {
        return false
    }
//    for (cl in AcceptableClasses) {
//        if (cl.isInstance(value)) {
//            return true
//        }
//    }
//    return false
    //因为我们总是把他们存在内存里，我们不需要检查类型
    return true
}

/**
 * Contains Classes which can be stored inside [Bundle].
 *
 * Some of the classes are not added separately because:
 *
 * This classes implement Serializable:
 * - Arrays (DoubleArray, BooleanArray, IntArray, LongArray, ByteArray,
 *   FloatArray, ShortArray, CharArray, Array<Parcelable, Array<String>)
 * - ArrayList
 * - Primitives (Boolean, Int, Long, Double, Float, Byte, Short, Char) will
 *   be boxed when casted to Any, and all the boxed classes implements
 *   Serializable. This class implements Parcelable:
 * - Bundle
 *
 * Note: it is simplified copy of the array from SavedStateHandle
 * (lifecycle-viewmodel-savedstate).
 */
private val AcceptableClasses = arrayOf(
    Serializable::class.java,
//    Parcelable::class.java,
    String::class.java,
//    SparseArray::class.java,
//    Binder::class.java,
//    Size::class.java,
//    SizeF::class.java
)

private fun Bundle.toMap(): Map<String, List<Any?>> {
    val map = mutableMapOf<String, List<Any?>>()
    this.keySet().filterNotNull().forEach { key ->
        val list = (getData(key) ?: emptyList<Any?>()) as ArrayList<Any?>
        map[key] = list
    }
    return map
}

private fun Map<String, List<Any?>>.toBundle(): Bundle {
    val bundle = Bundle()
    forEach { (key, list) ->
        val arrayList = if (list is ArrayList<Any?>) list else ArrayList(list)
        bundle.setObjectFixed(key, arrayList)
    }
    return bundle
}


//<editor-fold desc="反射Bundle">
/**
 * 反射Bundle中的bundleData
 */
fun Bundle.setObjectFixed(key: String, value: Any?) {
    val map = this.fix()
    map.put(key, value)
}

/**
 * 反射Bundle中的bundleData
 */
fun Bundle.getData(key: String): Any? {
    val map = this.fix()
    return map.get(key)
}

fun Bundle.fix(): MutableMap<String, Any?> {
    val field = this.javaClass.getDeclaredField("bundleData")
    field.isAccessible = true
    val map = field.get(this) as MutableMap<String, Any?>
    return map
}
//</editor-fold>
