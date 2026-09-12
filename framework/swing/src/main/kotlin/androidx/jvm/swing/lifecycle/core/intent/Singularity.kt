package androidx.jvm.swing.lifecycle.core.intent

import androidx.compose.desktop.runtime.core.ServiceBooter
import androidx.jvm.swing.lifecycle.jFrame.JFrameManager
import androidx.jvm.system.di.InstanceContext
import org.slf4j.LoggerFactory

object Singularity {
    private val logger = LoggerFactory.getLogger(Singularity::class.java)

    fun boot(){
        ServiceBooter.bootstrap(logger)
        InstanceContext.get().run {
            //用于提前生成JFrameManager,否则JFrameLauncher不会立马注册到IntentProcessor
            get<JFrameManager>()
        }
    }
}
