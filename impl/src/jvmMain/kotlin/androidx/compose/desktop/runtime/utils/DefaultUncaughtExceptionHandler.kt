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
import java.awt.Component
import java.awt.Dialog
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
 * @param continueAction: (Throwable) -> Unit 当错误弹窗弹出，点击忽略按钮之后触发
 * @param closeAppAction: (Throwable) -> Unit 当错误弹窗弹出，点击确定按钮之后触发
 */
fun setUncaughtExceptionHandler(
    copyAction: ErrHandler? = null,
    continueAction: ErrHandler? = null,
    closeAppAction: ErrHandler? = null
) {
    DefaultUncaughtExceptionHandler.apply {
        this.copyAction = copyAction
        if (closeAppAction != null) {
            this.closeAppAction = closeAppAction
        }
        this.continueAction = continueAction
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
    var continueAction: ErrHandler? = null
    var closeAppAction: ErrHandler = { exit(true) }
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
        window?.let {
            buildDialog(throwable, it)
            //关闭当前窗口
            window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSED))
        } ?: buildDialog(throwable)
    }

    private fun buildDialog(
        throwable: Throwable,
        parentComponent: Window,
    ) {
        val copyButton = JButton("复制日志并关闭")
        val closeAppButton = JButton("关闭应用")

        copyButton.addActionListener {
            SwingExtension.writeClipboard(throwable.stackTraceToString())
            copyAction?.invoke(throwable)
            parentComponent.dispose()
            closeAppAction.invoke(throwable)
        }

        closeAppButton.addActionListener { e: ActionEvent? ->
            parentComponent.dispose()
            closeAppAction.invoke(throwable)
        }

        val options = arrayOf<Any>(copyButton, closeAppButton)
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

    private fun buildDialog(
        throwable: Throwable,
    ) {
        val copyButton = JButton("复制错误日志")
        val continueButton = JButton("忽略")
        val closeAppButton = JButton("关闭应用")

        copyButton.addActionListener {
            SwingExtension.writeClipboard(throwable.stackTraceToString())
            copyAction?.invoke(throwable)
            // 关闭弹窗
            SwingUtilities.getWindowAncestor(it.source as Component)?.dispose()
        }

        continueButton.addActionListener {
            continueAction?.invoke(throwable)
            // 关闭弹窗
            SwingUtilities.getWindowAncestor(it.source as Component)?.dispose()
        }

        closeAppButton.addActionListener { e: ActionEvent? ->
            closeAppAction.invoke(throwable)
        }


        val options = arrayOf<Any>(copyButton, continueButton, closeAppButton)
        // 手动创建 JOptionPane 并设置为非模态
        val pane = JOptionPane(
            "错误: ${throwable.message ?: "未知的错误"}",
            JOptionPane.ERROR_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            null,
            options,
            options[0]
        )
        val dialog = pane.createDialog( "我们遇到了一些问题")
        dialog.setModalityType(Dialog.ModalityType.MODELESS) //模态窗口会无法点击次窗口下面的窗口，知道关闭此窗口
        dialog.isVisible = true
    }

    //</editor-fold>

}


