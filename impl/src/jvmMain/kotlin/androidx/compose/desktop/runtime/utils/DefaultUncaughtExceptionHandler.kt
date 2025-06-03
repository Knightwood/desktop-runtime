@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.desktop.runtime.utils

import androidx.compose.desktop.runtime.core.exit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import com.github.knightwood.slf4j.kotlin.logFor
import java.awt.Window
import java.awt.event.ActionEvent
import java.awt.event.WindowEvent
import java.lang.Thread.UncaughtExceptionHandler
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

private val logger = logFor("ErrorHandler")
typealias ErrHandler = (Throwable) -> Unit

/**
 * @param copyAction: (Throwable) -> Unit 当错误弹窗弹出，点击复制按钮之后触发
 * @param okAction: (Throwable) -> Unit 当错误弹窗弹出，点击确定按钮之后触发
 */
fun setUncaughtExceptionHandler(
    copyAction: ErrHandler? = null,
    okAction: ErrHandler? = null
) {
    DefaultUncaughtExceptionHandler.apply {
        this.copyAction = copyAction
        if (okAction != null) {
            this.okAction = okAction
        }
    }
    Thread.setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler)
}

/**
 * 传入自定义的弹窗，完全接管错误弹窗显示和处理
 */
fun setUncaughtExceptionHandler(
    dialog: (Window?, Throwable) -> Unit
) {
    DefaultUncaughtExceptionHandler.dialog = dialog
    Thread.setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler)
}

@Composable
fun UncaughtExceptionContent(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides DefaultUncaughtExceptionHandler) {
        content()
    }
}

/**
 * compose err handler factory
 *
 * jdk err handler
 *
 * 用法：
 *
 * ```
 * fun main() {
 *     Thread.setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler)
 *     application {
 *         CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides ComposeErrHandlerFactory) {
 *
 *         }
 *     }
 * }
 *
 * ```
 */
object DefaultUncaughtExceptionHandler : UncaughtExceptionHandler, WindowExceptionHandlerFactory {
    var copyAction: ErrHandler? = null
    var okAction: ErrHandler = { exit(true) }
    var dialog: (Window?, Throwable) -> Unit = ::showErrDialog

    //<editor-fold desc="java错误捕获">

    /**
     * main函数执行初期，抛出异常： 我们会在这里弹窗提示，当关闭我们的弹窗之后 如果最终进程退出，就会弹出 failed to launch
     * jvm 弹窗 如果最终进程活下来，就不会弹出 failed to launch jvm 弹窗
     */
    override fun uncaughtException(t: Thread, e: Throwable) {
        logger.error("Uncaught exception:", e)
        dialog(null, e)
    }

    //</editor-fold>

    //<editor-fold desc="compose错误处理">

    /**
     * provide compose err handler
     */
    override fun exceptionHandler(window: Window): WindowExceptionHandler {
        return WindowExceptionHandler { throwable ->
            logger.error("Uncaught exception:", throwable)
            SwingUtilities.invokeLater {
                dialog(window, throwable)
            }
//            throw throwable //不再继续抛出，以免被上面的uncaughtException方法处理
        }
    }

    fun showErrDialog(
        window: Window?, throwable: Throwable,
    ) {
        buildDialog(throwable, window?.takeIf { it.isDisplayable })
        //关闭当前窗口
        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSED))
    }

    private fun buildDialog(
        throwable: Throwable,
        parentComponent: Window?,
    ) {
        val okButton = JButton("复制错误日志")
        val cancelButton = JButton("关闭")

        okButton.addActionListener {
            parentComponent?.dispose()
            SwingExtension.writeClipboard(throwable.stackTraceToString())
            copyAction?.invoke(throwable)
        }

        cancelButton.addActionListener { e: ActionEvent? ->
            parentComponent?.dispose()
            okAction.invoke(throwable)
        }

        val options = arrayOf<Any>(okButton, cancelButton)
        JOptionPane.showOptionDialog(
            /* parentComponent = */ parentComponent,
            /* message = */ "错误: ${throwable.message ?: "未知的错误"}",
            /* title = */ "我们遇到了一些问题",
            /* optionType = */ JOptionPane.DEFAULT_OPTION,
            /* messageType = */ JOptionPane.ERROR_MESSAGE,
            /* icon = */ null,
            /* options = */ options,
            /* initialValue = */ options[0]
        )
    }

    //</editor-fold>

}


