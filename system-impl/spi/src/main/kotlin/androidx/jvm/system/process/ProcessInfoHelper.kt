package androidx.jvm.system.process

import androidx.jvm.system.model.ProcessSampleInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object ProcessInfoHelper {
    private val logger: Logger = LoggerFactory.getLogger(ProcessInfoHelper::class.java)

    private var cache : ProcessSampleInfo ?=null

    val sampleInfo: ProcessSampleInfo
        get() {
            if (cache != null) {
                return cache!!
            }
            val sampleInfo = ProcessSampleInfo()
            // 获取当前进程的 ProcessHandle
            val currentProcess = ProcessHandle.current()

            // 获取进程信息
            val processInfo = currentProcess.info()

            // 获取命令行
            processInfo.command().ifPresent(
                Consumer { cmd: String? ->
                    sampleInfo.command = cmd
                })

            // 获取命令行参数
            processInfo.arguments().ifPresent(
                Consumer { args ->
                    sampleInfo.arguments = args
                }
            )

            // 获取进程用户
            processInfo.user().ifPresent(
                Consumer { user: String? ->
                    sampleInfo.user = user
                }
            )

            // 获取完整命令行参数
            processInfo.commandLine().ifPresent(
                Consumer { cmdLine: String? ->
                    sampleInfo.commandLine = cmdLine
                }
            )
            // 获取进程 PID
            sampleInfo.pid = currentProcess.pid()

            // 获取MainClass
            sampleInfo.mainClassString = getMainClass()

            return sampleInfo
        }

    private fun getMainClass(): String {
        val stackTrace = Thread.currentThread().getStackTrace()

        // 遍历堆栈找到 main 方法
        for (element in stackTrace) {
            if ("main" == element.getMethodName()) {
                return element.getClassName()
            }
        }


        // 备选方案：获取最底层的栈帧（有时main方法不直接可见）
        val fullStackTrace = Thread.currentThread().getStackTrace()
        if (fullStackTrace.size > 0) {
            return fullStackTrace[fullStackTrace.size - 1].getClassName()
        }

        return "Unknown"
    }

    /**
     * 传入执行进程路径，获取进程名
     */
    fun parseAppName(path: String?): String {
        // 提取文件名
        val parts: Array<String?> =
            (path?:"Unknow")
                .replace("\\", "/")
                .split("/".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        val exename = parts[parts.size - 1] ?: "Unknow"
        return if (exename.contains(".")) {
            exename.take(exename.indexOf("."))
        } else {
            exename
        }
    }
}
