package androidx.jvm.system.process

import com.github.knightwood.slf4j.kotlin.logFor
import java.nio.channels.*
import java.nio.file.*

object ProcessLocker {
    private lateinit var fileChannel: FileChannel
    private val logger = logFor("ProcessLocker")

    /**
     * 获取文件锁，如果无法获取则抛出异常。
     * 非阻塞，不需要放进子线程中。
     */
    fun lock(path: Path) {
        // 防止重复加锁
        if (this::fileChannel.isInitialized) return

        val file = path.toFile()
        if (!file.exists()) {
            file.createNewFile()
        }

        // 打开文件通道
        val channel = FileChannel.open(
            path,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE
        )

        // 尝试获取锁
        val lock = channel.tryLock()

        if (lock == null) {
            logger.info("无法获取文件锁：另一个进程正在运行")
            channel.close()
            throw RuntimeException("Process already running - cannot acquire lock")
        }
        // 成功获取锁后，保存 channel 并不释放锁
        fileChannel = channel
    }

    /**
     * 可选：手动释放锁（通常不需要调用）
     */
    fun unlock() {
        if (!this::fileChannel.isInitialized) return
        if (!fileChannel.isOpen) return
        fileChannel.close()
    }
}
