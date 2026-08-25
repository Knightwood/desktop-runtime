package androidx.compose.desktop.runtime.savestate

import androidx.savedstate.SavedState
import androidx.savedstate.savedState

/**
 * 整个应用所有activity的SaveState都会保存在这里
 * 用户可以从此类获取所有activity保存的状态,以便将状态保存到磁盘,
 * 下次启动application后可以将保存到磁盘的状态放入此管理类,方便启动activity后恢复状态
 */
class ApplicationSaveStateSaver {
    private val allSavedState: MutableMap<Token, SavedState> = mutableMapOf()

    internal fun getSaveState(token: Token): SavedState {
        return allSavedState.getOrPut(token, { savedState() })
    }

    fun remove(token: Token) = allSavedState.remove(token)

    fun release() = allSavedState.clear()

    /**
     * 将状态数据导出,方便用户将其存储到磁盘
     */
    fun export(): Map<Token, SavedState> {
        return allSavedState
    }

    /**
     * 导入保存到磁盘的状态数据
     * 此函数调用时机需要早于第一个activity的启动时机
     */
    fun import(data: Map<Token, SavedState>): ApplicationSaveStateSaver {
        this.allSavedState.clear()
        this.allSavedState.putAll(data)
        return this
    }

}
