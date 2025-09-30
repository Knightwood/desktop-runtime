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
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.savedState
import java.io.Serializable

/**
 * 用于自定义compose component，比如fragment、dialog fragment等。
 * 这些组件中的compose函数生命周期理应追随组件本身的生命周期，
 * 因此需要覆盖掉composeContainer（顶层的Window函数）提供的LifecycleOwner
 * @param lifecycleOwner 组件生命周期,用于覆盖掉composeContainer提供的LifecycleOwner
 */
@Composable
fun ProvideAndroidCompositionLocalsForDialog(
    id: String,
    context: IContext?,
    lifecycleOwner: LifecycleOwner,
    viewModelStoreOwner: ViewModelStoreOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    content: @Composable () -> Unit,
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
        LocalContext provides (context ?: LocalContext.current),
        LocalLifecycleOwner provides lifecycleOwner,
        LocalViewModelStoreOwner provides viewModelStoreOwner,
        LocalSaveableStateRegistry provides saveableStateRegistry,
        LocalSavedStateRegistryOwner provides savedStateRegistryOwner,
    ) {
        content()
    }
}

/**
 * 功能类似android中调用setContent时，其内部提供LocalSaveableStateRegistry。
 * * compose 1.8.0-alpha04之前：数据存储恢复使用的是Bundle类
 * * compose
 *   1.8.0-alpha04之后：数据存储恢复使用的是SavedState类，与之前的bundle类相似，都是内部持有map实现功能，但是限制更少，使用更方便。
 *
 * 需要注意的是，迁移到SavedState类并没有实现SaveState存储和恢复api，这一部分仍需要我们自己实现。
 *
 * 需要引入:
 * ```
 * "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.0-alpha05"
 * "org.jetbrains.androidx.savedstate:savedstate:1.3.0-alpha05"
 * ```
 *
 * 如果你想用bundle类，需要额外引入
 *
 * ```
 * "org.jetbrains.androidx.core:core-bundle:"1.1.0-alpha03"
 *
 * ```
 * 我们activity的生命周期实际上观察composeContainer（顶层的Window函数）提供的Lifecycle，
 * 因此我们不能覆盖掉LocalLifecycleOwner，
 * 在这里只需要使用[ActivityLifecycleOwner]提供activity的lifecycleOwner即可
 *
 * @param activityLifecycleOwner activity的lifecycleOwner
 */
@Composable
fun ProvideAndroidCompositionLocals(
    id: String,
    context: IContext?,
    activityLifecycleOwner: LifecycleOwner,
    viewModelStoreOwner: ViewModelStoreOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    content: @Composable () -> Unit,
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
        LocalContext provides (context ?: LocalContext.current),
        ActivityLifecycleOwner provides activityLifecycleOwner,
        LocalViewModelStoreOwner provides viewModelStoreOwner,
        LocalSaveableStateRegistry provides saveableStateRegistry,
        LocalSavedStateRegistryOwner provides savedStateRegistryOwner,
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
    savedStateRegistryOwner: SavedStateRegistryOwner,
): DisposableSaveableStateRegistry {
    val key = "${SaveableStateRegistry::class.java.simpleName}:$id"

    //获取从activity中的savedStateRegistry
    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    //使用key得到之前保存的数据，用于activity onCreate时恢复数据，
    val bundle = androidxRegistry.consumeRestoredStateForKey(key)
    val restored: Map<String, List<Any?>>? = bundle?.toMap() as Map<String, List<Any?>>?

    // 最终返回此实例，并使用CompositionLocalProvider提供此实例，如此：
    // compose会将数据保存进去，也会从这里读取数据用于恢复。
    //  activity保存数据时会回调此实例提供的SavedStateProvider
    // compose生成界面时，触发了函数内部rememberSavable，他只执行一次，用于数据恢复
    val saveableStateRegistry = SaveableStateRegistry(restoredValues = restored, canBeSaved = ::canBeSavedToBundle)
    val registered = try {
        //注册SavedStateProvider，当activity触发保存数据时，会调用此处注册的SavedStateProvider，得到界面需要保存的数据
        androidxRegistry.registerSavedStateProvider(key) {
            saveableStateRegistry.performSave().toSaveState()
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
    private val onDispose: () -> Unit,
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

private fun Map<String, List<Any?>>.toSaveState(): SavedState {
    return savedState(this)
}

//<editor-fold desc="反射SavedState">
/**
 * 反射SavedState，获取map
 */
fun SavedState.toMap(): Map<String, Any?> {
    return getSaved()
}

/**
 * 使用反射获取SavedState中的map
 */
fun SavedState.getSaved(): MutableMap<String, Any?> {
    val field = this.javaClass.getDeclaredField("map")
    field.isAccessible = true
    val map = field.get(this) as MutableMap<String, Any?>
    return map
}

/**
 * 将两个SavedState的数据合并
 */
fun SavedState.merge(other: SavedState): SavedState {
    this.getSaved().putAll(other.toMap())
    return this
}

//</editor-fold>
//<editor-fold desc="反射Bundle">
/**
 * 反射Bundle中的bundleData，将数据放入Bundle
 */
fun Bundle.setObjectFixed(key: String, value: Any?) {
    val map = this.getSaved()
    map.put(key, value)
}

/**
 * 反射Bundle中的bundleData，获取map中存储的数据
 */
fun Bundle.getData(key: String): Any? {
    val map = this.getSaved()
    return map.get(key)
}

/**
 * 反射Bundle，获取bundle中存储数据的map
 */
fun Bundle.getSaved(): MutableMap<String, Any?> {
    val field = this.javaClass.getDeclaredField("bundleData")
    field.isAccessible = true
    val map = field.get(this) as MutableMap<String, Any?>
    return map
}
//</editor-fold>
