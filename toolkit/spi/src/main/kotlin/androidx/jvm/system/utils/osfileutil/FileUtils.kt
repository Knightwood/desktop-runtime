package androidx.jvm.system.utils.osfileutil

import androidx.jvm.system.utils.SystemOs
import androidx.jvm.system.utils.currentOS
import java.io.File

interface FileUtils {
    fun openFile(file: File): Boolean
    fun openFolderOfFile(file: File): Boolean
    fun openFolder(folder: File): Boolean
    fun canWriteInThisFolder(folder: String): Boolean
    fun isRemovableStorage(path: String): Boolean

    companion object : FileUtils by getPlatformFileUtil()
}

private fun getPlatformFileUtil(): FileUtils {
    return when (currentOS) {
        SystemOs.Windows -> WindowsFileUtils()
        SystemOs.Linux -> LinuxFileUtils()
        SystemOs.MacOS -> MacOsFileUtils()
        SystemOs.Android -> JVMFileUtils()
    }
}
