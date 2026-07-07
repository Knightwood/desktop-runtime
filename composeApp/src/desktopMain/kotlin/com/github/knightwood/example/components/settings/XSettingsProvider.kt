package com.github.knightwood.example.components.settings

import androidx.datastore.core.DataStore
import com.github.knightwood.example.components.render.SkikoRenderApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object XSettingsProvider : KoinComponent {
    val ds by inject<DataStore<XSettings>>(named("xSettingsDS"))

    val flow = ds.data

    fun defaultValue() = XSettings()

    suspend fun update(entity: XSettings) {
        ds.updateData { entity }
    }

    suspend fun query(): XSettings {
        return ds.data.first()
    }

    fun read(): XSettings {
        return runBlocking { query() }
    }

}

@Serializable
data class XSettings(
    /**
     * 每个单体检定页面中都有一个swing组件的曲线图表，单体检定系统swing组件位置固定，不会有伪影。
     * 群控版本切换检定组选项卡，检定页面内容会产生动画、swing组件会重组、位移，
     * 如果不启用swing组件互作混合、不用D3D、Vulkan渲染api，swing组件会有伪影，很不协调。
     * 所以需要在群控版本中启用swing组件互作混合、且使用至少是Vulkan的渲染api。
     * 群控版本需要特制硬件，肯定不会用垃圾工控电脑（仅支持software渲染api）和扫描器一体的检定箱，
     * 因此渲染api不会受限于只能用software api，可以默认使用D3D
     */
    val skikoRenderApi: SkikoRenderApi = SkikoRenderApi.DIRECT3D,
    val singleInstance: Boolean = true,
    val closeAppDirectly: Boolean = true,
)
