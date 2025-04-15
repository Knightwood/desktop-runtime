package androidx.compose.desktop.runtime.domain

import androidx.compose.desktop.runtime.context.noLocalProvidedFor
import androidx.compose.desktop.runtime.utils.CompositionLocalProviderNullable
import androidx.compose.desktop.runtime.utils.providesNullable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.random.Random

val LocalSavedStateRegistryOwner = staticCompositionLocalOf<SavedStateRegistryOwner> {
    noLocalProvidedFor("LocalSavedStateRegistryOwner")
}


/**
 * 功能类似于NavHost，可以提供状态保存和恢复功能，以正常使用rememberSavable
 * 需要注意，每个使用此方法的compose函数，都需要一个独立的ViewModelStoreOwner，他们不能重复。
 * 就像NavHost中，每个导航回退节点对应一个页面，每个导航回退节点都是一个ViewModelStoreOwner。
 *
 * ```
 *class ViewModelStoreOwnerImpl : ViewModelStoreOwner {
 *     override val viewModelStore: ViewModelStore
 *         get() = TODO("Not yet implemented")
 * }
 *
 * class Activity1 : Activity() {
 *     val owner1 = ViewModelStoreOwnerImpl()
 *     val owner2 = ViewModelStoreOwnerImpl()
 *     override fun onCreate(savedInstanceState: SavedState?) {
 *         super.onCreate(savedInstanceState)
 *         setContent {
 *             Screen()
 *         }
 *     }
 *
 *     //不可以共用Activity这个ViewModelStoreOwner，
 *     //两个compose页面如果使用同一个ViewModelStoreOwner，
 *     //会导致无法正常恢复数据。
 *     @Composable
 *     fun Screen() {
 *         Column {
 *             ScreenA(owner1) {}
 *             HorizontalDivider()
 *             ScreenA(owner2) {}
 *         }
 *     }
 * }
 *
 *
 * @Composable
 * fun ScreenA(owner: ViewModelStoreOwnerImpl, content: @Composable () -> Unit) {
 *     NavHostSaveStateProvider(owner) {
 *         content()
 *     }
 * }
 * ```
 *
 * @param viewModelStoreOwner 必须传，页面保存恢复状态，就指望着viewmodel呢。
 * @param lifecycleOwner 可选
 * @param savedStateRegistryOwner 可选
 * @param content
 */
@Composable
fun NavHostSaveStateProvider(
    viewModelStoreOwner: ViewModelStoreOwner,
    lifecycleOwner: LifecycleOwner? = null,
    savedStateRegistryOwner: SavedStateRegistryOwner? = null,
    content: @Composable () -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    CompositionLocalProviderNullable(
        LocalViewModelStoreOwner provides viewModelStoreOwner,
        LocalLifecycleOwner providesNullable lifecycleOwner,
        LocalSavedStateRegistryOwner providesNullable savedStateRegistryOwner
    ) {
        saveableStateHolder.SaveableStateProvider(content)
    }
}

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal class WeakReference<T : Any> constructor(reference: T) {
    private val javaReference = java.lang.ref.WeakReference(reference)
    actual fun get(): T? = javaReference.get()
    actual fun clear() {
        javaReference.clear()
    }
}


@Composable
private fun SaveableStateHolder.SaveableStateProvider(content: @Composable () -> Unit) {
    val viewModel = viewModel(
        // TODO investigate why inline with refined type triggers
        //  "Compilation failed: Symbol for ... is unbound"
        //  https://github.com/JetBrains/compose-multiplatform/issues/3147
        BackStackEntryIdViewModel::class,
        factory = viewModelFactory {
            initializer { BackStackEntryIdViewModel(createSavedStateHandle()) }
        }
    )
    // Stash a reference to the SaveableStateHolder in the ViewModel so that
    // it is available when the ViewModel is cleared, marking the permanent removal of this
    // NavBackStackEntry from the back stack. Which, because of animations,
    // only happens after this leaves composition. Which means we can't rely on
    // DisposableEffect to clean up this reference (as it'll be cleaned up too early)
    viewModel.saveableStateHolderRef = WeakReference(this)
    SaveableStateProvider(viewModel.id, content)
}

@OptIn(ExperimentalStdlibApi::class)
private fun randomUUID(): String {
    val bytes = Random.nextBytes(16).also {
        it[6] = it[6] and 0x0f // clear version
        it[6] = it[6] or 0x40 // set to version 4
        it[8] = it[8] and 0x3f // clear variant
        it[8] = it[8] or 0x80.toByte() // set to IETF variant
    }
    return StringBuilder(36)
        .append(bytes.toHexString(0, 4))
        .append('-')
        .append(bytes.toHexString(4, 6))
        .append('-')
        .append(bytes.toHexString(6, 8))
        .append('-')
        .append(bytes.toHexString(8, 10))
        .append('-')
        .append(bytes.toHexString(10))
        .toString()
}

fun randomId(): String = randomUUID()

/**
 * 在NavHost中 一个回退栈节点对应一个导航页面，回退栈节点就是ViewmodelStoreOwner，
 * 自然一个导航页面对应一个BackStackEntryIdViewModel，对于这个页面永远不会有第二个实例。
 * 对于两个导航页面，获取的viewmodel也永远不会重复和混淆。
 * 每个页面对应的唯一的viewModel跟着回退栈节点同生共死，自然id可以是个随机数。
 *
 * 持有SaveableStateHolder和用于页面数据保存恢复的key
 */
internal class BackStackEntryIdViewModel(handle: SavedStateHandle) : ViewModel() {

    private val IdKey = "SaveableStateHolder_BackStackEntryKey"

    // we create our own id for each back stack entry to support multiple entries of the same
    // destination. this id will be restored by SavedStateHandle
    val id: String = handle.get<String>(IdKey) ?: randomId().also {
        handle.set(IdKey, it)
    }

    lateinit var saveableStateHolderRef: WeakReference<SaveableStateHolder>

    // onCleared will be called on the entries removed from the back stack. here we notify
    // SaveableStateProvider that we should remove any state is had associated with this
    // destination as it is no longer needed.
    override fun onCleared() {
        super.onCleared()
        saveableStateHolderRef.get()?.removeState(id)
        saveableStateHolderRef.clear()
    }
}

