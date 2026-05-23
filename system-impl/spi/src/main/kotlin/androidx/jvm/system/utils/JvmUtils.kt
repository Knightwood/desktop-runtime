package androidx.jvm.system.utils

import java.io.File
import java.net.URISyntaxException

object JvmUtils {

    /**
     * 获取main函数所在的类
     */
    fun getMainClass(): Class<*>? {
        return StackWalker.getInstance().walk { stream ->
            stream.filter { frame ->
                "main".equals(frame.getMethodName())
            }.findFirst().map(StackWalker.StackFrame::getDeclaringClass).orElse(null)
        }
    }

    /**
     * 获取给定类所在的JAR文件路径
     *
     * 如果类位于JAR文件内，返回该JAR文件的绝对路径
     *
     * 如果类不在JAR文件内，返回其所在目录的绝对路径
     *
     * @param clazz 用于查找JAR路径的类
     * @return JAR文件或类所在目录的绝对路径
     * @throws RuntimeException 如果无法获取JAR路径，则抛出运行时异常
     *
     * 这些方法在IDE中直接运行时可能不会返回JAR路径，而是返回class文件所在目录
     *
     * 在Windows系统中，路径可能以"/"开头（如"/C:/path/to/jar"），需要适当处理
     *
     * 如果JAR文件在jar-in-jar（嵌套JAR）中，这些方法可能不适用
     *
     * 对于Web应用或特殊类加载器环境，可能需要其他方法
     */
    fun getJarPath(clazz: Class<*>): String {
        try {
            val jarFile = File(
                clazz.protectionDomain
                    .codeSource
                    .location
                    .toURI()
            )
            return if (jarFile.isFile) {
                jarFile.absolutePath
            } else {
                jarFile.parentFile.absolutePath
            }
        } catch (e: URISyntaxException) {
            throw RuntimeException("无法获取JAR路径", e)
        }
    }
}
