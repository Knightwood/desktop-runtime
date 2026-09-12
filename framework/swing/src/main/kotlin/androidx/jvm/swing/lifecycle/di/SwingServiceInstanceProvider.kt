package androidx.jvm.swing.lifecycle.di

import androidx.jvm.swing.lifecycle.jFrame.JFrameLauncher
import androidx.jvm.swing.lifecycle.jFrame.JFrameManager
import androidx.jvm.system.di.ModuleProvider
import com.google.auto.service.AutoService
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.swing.SwingDispatcher
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

@AutoService(ModuleProvider::class)
class SwingServiceInstanceProvider : ModuleProvider {

    override fun provide(): Module {
        return module {
            singleOf(::JFrameManager)
            single { get<JFrameManager>().launcherManager }

            single(named<JFrameManager>()) {
                CoroutineScope(Dispatchers.Swing) + SupervisorJob() + CoroutineName("JFrameManager")
            }
        }
    }
}
