package com.github.knightwood.example.components

import androidx.jvm.system.process.ProcessLocker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.github.knightwood.example.path.FilePathProvider
import javax.swing.JOptionPane

/**
 * 提供一些app的运行状态，比如当前是否运行检定任务
 */
object AppStateHolder {
    private val coroutineScope= CoroutineScope(Dispatchers.Default)
    private var exitAction: IOperateAction<Any>? = null

    /**
     * 探测是否已经存在运行的进程，如果存在，则显示提示弹窗
     */
    fun probe(action: () -> Unit) {
        try {
            ProcessLocker.lock(FilePathProvider.lockFilePath.toNioPath())
        } catch (e: Exception) {
            alertSingleProcessError(action)
        }
    }

    private fun alertSingleProcessError(action: () -> Unit) {
        JOptionPane.showMessageDialog(
            null, // 父组件，null 表示居中显示在屏幕中央
            "程序已在运行中，不可重复启动。\n" +
                    "(可以点击系统托盘图标回到程序。)",
            "提示", // 弹窗标题
            JOptionPane.WARNING_MESSAGE // 消息类型（错误图标）
        )
        //swing弹窗似乎是会阻塞线程的，这里的action会在关闭弹窗后执行
        action()
    }

    /**
     * 在没有application的环境中退出app
     */
    fun exit(data: Any = Any()) {
        exitAction?.invoke(data)
    }

    /**
     * 提供一段逻辑，用于在无application的环境下退出app 退出app的函数[exit]
     */
    fun registerExitAction(action: IOperateAction<Any>) {
        exitAction = action
    }

}


fun interface IOperateAction<T> : (T) -> Unit {
    override fun invoke(data: T)
}
