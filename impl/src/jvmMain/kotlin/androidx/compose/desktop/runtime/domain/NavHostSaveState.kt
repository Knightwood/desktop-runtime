package androidx.compose.desktop.runtime.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.random.Random




/**
 * 功能类似于NavHost中提供的LocalSaveableStateRegistry
 */
@Composable
fun NavHostSaveStateProvider(
    content: @Composable () -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    saveableStateHolder.SaveableStateProvider(content)
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

