package androidx.compose.desktop.runtime.savestate

import androidx.savedstate.SavedState
import androidx.savedstate.savedState

/**
 * 整个应用所有Activity、IComponent的SaveState都会保存在这里
 * 用户可以从此类获取所有Activity、IComponent保存的状态,以便将状态保存到磁盘,
 * 下次启动Application后可以将保存到磁盘的状态放入此管理类,方便启动Activity、IComponent后恢复状态
 */
class ApplicationSaveStateSaver {
    private val allSavedState: MutableMap<Token, SavedState> = mutableMapOf()

    /**
     * 注册并返回与token关联的SavedState实例
     *
     * @param token
     * @return SavedState实例
     */
    fun obtain(token: Token): SavedState {
        return allSavedState.getOrPut(token, { savedState() })
    }

    /**
     * java兼容方法
     */
    fun obtain(token: IToken): SavedState {
        return obtain(Token(token.value))
    }

    /**
     * 删除与token关联的SavedState
     */
    fun remove(token: Token) = allSavedState.remove(token)

    /**
     * java兼容方法
     */
    fun remove(token: IToken) = allSavedState.remove(Token(token.value))

    /**
     * 删除全部的SavedState实例
     */
    fun release() = allSavedState.clear()

    /**
     * 将状态数据导出,方便用户将其存储到磁盘
     */
    fun export(): Map<Token, SavedState> {
        return allSavedState
    }

    /**
     * 导入保存到磁盘的状态数据
     * 此函数调用时机需要早于第一个Activity、IComponent的启动时机
     */
    fun import(data: Map<Token, SavedState>): ApplicationSaveStateSaver {
        this.allSavedState.clear()
        this.allSavedState.putAll(data)
        return this
    }

}
