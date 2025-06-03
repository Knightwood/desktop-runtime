package androidx.compose.desktop.runtime.utils.err

import java.awt.*
import javax.swing.JOptionPane

object SwingErrorDialog {

    fun showErrorDialog2( e: Throwable) {
        JOptionPane.showMessageDialog(
            null, // 父组件，null 表示居中显示在屏幕中央
            e.stackTraceToString(), // 错误信息
            "Error", // 弹窗标题
            JOptionPane.ERROR_MESSAGE // 消息类型（错误图标）
        )
    }

    fun showErrorDialog(e: Throwable) {
        Dialog(Frame(), e.message ?: "Error").apply {
            layout = FlowLayout()
            val label = Label(e.message)
            add(label)
            val button = Button("OK").apply {
                addActionListener { dispose() }
            }
            add(button)
            setSize(500, 260)
            isVisible = true
        }
    }
}
