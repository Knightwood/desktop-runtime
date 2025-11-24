package androidx.compose.desktop.runtime.activity

import androidx.savedstate.SavedState

interface ISaveStateHolder {
    fun obtainSaveState(uuid: String): SavedState
    fun obtainSavestateNullable(uuid: String): SavedState?
    fun clearSaveState(uuid: String)
    fun clear()
    fun setSaveState(uuid: String, savedState: SavedState)
}