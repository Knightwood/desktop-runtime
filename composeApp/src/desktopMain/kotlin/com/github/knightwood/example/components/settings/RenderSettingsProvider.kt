package com.github.knightwood.example.components.settings

import androidx.datastore.core.DataStore
import com.github.knightwood.example.components.render.SkikoRenderApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object RenderSettingsProvider : KoinComponent {
    val ds by inject<DataStore<RenderSettings>>(named("xSettingsDS"))

    val flow = ds.data

    fun defaultValue() = RenderSettings()

    suspend fun update(entity: RenderSettings) {
        ds.updateData { entity }
    }

    suspend fun query(): RenderSettings {
        return ds.data.first()
    }

    fun read(): RenderSettings {
        return runBlocking { query() }
    }

}

@Serializable
data class RenderSettings(
    val skikoRenderApi: SkikoRenderApi = SkikoRenderApi.DIRECT3D,
    val singleInstance: Boolean = true,
    val closeAppDirectly: Boolean = true,
)
