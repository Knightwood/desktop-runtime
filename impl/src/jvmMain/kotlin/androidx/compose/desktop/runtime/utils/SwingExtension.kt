package androidx.compose.desktop.runtime.utils

object SwingExtension {

    fun writeClipboard(text: String) {
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val transferable = java.awt.datatransfer.StringSelection(text)
        clipboard.setContents(transferable, null)
    }

    fun readClipboard(): String {
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val transferable = clipboard.getContents(null)
        return transferable.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as String? ?: ""
    }
}
