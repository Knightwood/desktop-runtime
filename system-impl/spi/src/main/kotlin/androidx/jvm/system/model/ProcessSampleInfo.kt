package androidx.jvm.system.model

import androidx.jvm.system.process.ProcessInfoHelper
import androidx.jvm.system.utils.JvmUtils

data class ProcessSampleInfo(
    var command: String? = null,
    var commandLine: String? = null,
    var pid: Long = 0L,
    var arguments: Array<String> = emptyArray(),
    var user: String? = null,
    var mainClassString: String = "",
) {
    val mainClass: Class<*>
        get() {
            return Class.forName(mainClassString)
        }

    val mainJarPath: String
        get() {
            return JvmUtils.getJarPath(mainClass)
        }

    val mainJarName
        get() = ProcessInfoHelper.parseAppName(
            JvmUtils.getJarPath(mainClass)
        )

    val processName: String
        get() {
            return ProcessInfoHelper.parseAppName(command)
        }

    val isProcessNameIsJava get() = command!!.contains("bin\\java")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessSampleInfo

        if (pid != other.pid) return false
        if (command != other.command) return false
        if (commandLine != other.commandLine) return false
        if (!arguments.contentEquals(other.arguments)) return false
        if (user != other.user) return false
        if (processName != other.processName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pid.hashCode()
        result = 31 * result + (command?.hashCode() ?: 0)
        result = 31 * result + (commandLine?.hashCode() ?: 0)
        result = 31 * result + arguments.contentHashCode()
        result = 31 * result + (user?.hashCode() ?: 0)
        result = 31 * result + processName.hashCode()
        return result
    }
}
