@file:Suppress("UNCHECKED_CAST")

package androidx.compose.desktop.runtime.savestate

import androidx.compose.desktop.runtime.activity.ActivityLifecycleOwner
import androidx.compose.desktop.runtime.activity.FragmentLifecycleOwner
import androidx.compose.desktop.runtime.core.context.Context
import androidx.compose.desktop.runtime.core.context.LocalContext
import androidx.compose.desktop.runtime.utils.CompositionLocalProviderNullable
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
 * SavedStateRegistryOwner: activity实现此接口提供SavedStateRegistry
 * SavedStateRegistry: 最顶层的SaveState数据容器
 *      提供ViewModel的SaveStateHandle、各View的状态保存恢复等功能基础(他们都会生成SaveStateProvider,注册进SavedStateRegistry)
 *      1. 收集所有注册的SaveStateProvider提供的需要保存的数据,将其保存到系统提供的outBundle
 *      2. onCreate时从savedInstanceState中获取上次保存的数据,分发给所有注册的SaveStateProvider以便恢复数据
 * LocalSavedStateRegistryOwner: 提供SavedStateRegistry
 *
 * SaveableStateRegistry: compose view中的顶层数据容器, 需要注册到SavedStateRegistry,无法独立存在
 *      1. 提供与SavedStateRegistry类似的功能,收集所有rememberSavable注册的SaveStateProvider提供的需要保存的数据
 *      2. 从SavedStateRegistry中获取保存的数据,分发给所有rememberSavable注册的SaveStateProvider以便恢复数据
 * LocalSaveableStateRegistry: 提供SaveableStateRegistry
 *      LocalSaveableStateRegistry有了值，rememberSavable才能获取到SaveableStateRegistry,实现保存和恢复数据
 *
 * 此方法复制并修改自androidx.compose.ui包, activity中setContent函数的部分流程
 *
 * 注:
 * compose 1.8.0-alpha04之前：数据存储恢复使用的是Bundle类
 * 现在为了跨平台, commonMain中抽象出来expect class SaveState,
 * 在android平台SavedState是Bundle类的别名.
 * 在jvm平台,SavedState类的有了具体实现, 是个持有hashMap的普通类.
 *
 * compose
 *   1.8.0-alpha04之后：数据存储恢复使用的是SavedState类，与之前的bundle类相似，都是内部持有map实现功能，但是限制更少，使用更方便。
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
 *
 * 我们activity的生命周期实际上观察composeContainer（顶层的Window函数）提供的Lifecycle，
 * 因此我们不能覆盖掉LocalLifecycleOwner，
 * 需要使用[androidx.compose.desktop.runtime.activity.ActivityLifecycleOwner]提供activity的lifecycleOwner
 *
 * @param id 标识SaveableStateRegistry唯一性,SaveableStateRegistry需要注册进SavedStateRegistry,如果id不唯一,会造成冲突
 * @param activityLifecycleOwner activity的lifecycleOwner
 */
@Composable
fun ProvideAndroidCompositionLocals(
    id: String,
    context: Context?,
    activityLifecycleOwner: LifecycleOwner?,
    fragmentLifecycleOwner: LifecycleOwner? = null,
    viewModelStoreOwner: ViewModelStoreOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    content: @Composable () -> Unit,
) {
    val saveableStateRegistry = remember {
        DisposableSaveableStateRegistry(id, savedStateRegistryOwner)
    }
    DisposableEffect(Unit) {
        onDispose {
            saveableStateRegistry.onDispose.invoke()
        }
    }
    CompositionLocalProviderNullable(
        LocalContext provides (context ?: LocalContext.current),
        ActivityLifecycleOwner provides activityLifecycleOwner,
        FragmentLifecycleOwner provides fragmentLifecycleOwner,
        LocalViewModelStoreOwner provides viewModelStoreOwner,
        LocalSaveableStateRegistry provides saveableStateRegistry.value,
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
 *
 * 调用此方法,立即进行一次数据恢复,并生成一个SaveableStateRegistry实例,注册进SavedStateRegistry.
 * 使用此方法时需要在activity onCreate SavedStateRegistry恢复数据之后,
 * 且此方法在全生命周期中只可调用一次,如果在compose中调用,需要使用remember包裹.
 *
 * 使用方式:
 * ```
 * val saveableStateRegistry = remember {
 *     DisposableSaveableStateRegistry(id, savedStateRegistryOwner)
 * }
 * DisposableEffect(Unit) {
 *     onDispose {
 *         saveableStateRegistry.onDispose.invoke()
 *     }
 * }
 * CompositionLocalProvider(
 *     LocalSaveableStateRegistry provides saveableStateRegistry.value,
 * ) {
 *     content()
 * }
 * ```
 */
internal fun DisposableSaveableStateRegistry(
    id: String,
    savedStateRegistryOwner: SavedStateRegistryOwner,
): DisposableSaveableStateRegistry {
    val key = "${SaveableStateRegistry::class.java.simpleName}:$id"

    //获取从activity中的savedStateRegistry
    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    //使用key得到之前保存的数据，立即进行恢复数据(compose第一次显示时,activity生命周期已经到了onCreate之后)
    val bundle = androidxRegistry.consumeRestoredStateForKey(key)
    val restored: Map<String, List<Any?>>? = bundle?.toMap() as Map<String, List<Any?>>?

    // 最终返回持有了SaveableStateRegistry的DisposableSaveableStateRegistry实例，并使用CompositionLocalProvider提供SaveableStateRegistry实例
    // compose中的rememberSavable会获取SaveableStateRegistry实例,实现数据保存和恢复。
    // SaveableStateRegistry最终会注册到activity的SavedStateRegistry
    val saveableStateRegistry = SaveableStateRegistry(restoredValues = restored, canBeSaved = ::canBeSavedToBundle)
    val registered = try {
        // 生成一个匿名SavedStateProvider实例,注册进SavedStateRegistry
        // 当系统触发activity中数据保存时, SavedStateRegistry会收集所有注册的SaveStateProvider中提供的数据
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
    return DisposableSaveableStateRegistry(
        value = saveableStateRegistry,
        onDispose = {
            if (registered) {
                androidxRegistry.unregisterSavedStateProvider(key)
            }
        }
    )
}

/**
 * [SaveableStateRegistry] which can be disposed using [dispose].
 */
internal data class DisposableSaveableStateRegistry(
    val value: SaveableStateRegistry,
    val onDispose: () -> Unit,
)

/**
 * Checks that [value] can be stored inside [Bundle].
 *
 * compose 1.8.0-alpha04之前：数据存储恢复使用的是Bundle类
 * 现在为了跨平台, commonMain中抽象出来expect class SaveState,
 * 在android平台SavedState是Bundle类的别名.
 * 在jvm平台,SavedState类的有了具体实现, 是个持有hashMap的普通类.
 *
 * 1. 由于这里的源码复制自android平台,所以名称中使用了Bundle.
 * 2. 虽然jvm平台SavedState类是个持有hashMap的普通类,可以存储任意类型,
 *   但为了能将SaveState保存到磁盘(android中配置变更\旋转屏幕才需要恢复状态,在jvm平野绫没这个需求,但是我想
 *   实现软件关闭后再次打开,恢复上一次的状态,这就需要将SaveState数据保存到磁盘),这里要对数据类型做限制.
 *
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
    //todo 感觉很难实现将savestate序列化到磁盘, 干脆放开类型限制
//    for (cl in AcceptableClasses) {
//        if (cl.isInstance(value)) {
//            return true
//        }
//    }
//    return false
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

//<editor-fold desc="SaveState处理">
private fun Map<String, List<Any?>>.toSaveState(): SavedState {
    return savedState(this)
}
//</editor-fold>
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
//<editor-fold desc="Bundle处理">

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
//</editor-fold>
