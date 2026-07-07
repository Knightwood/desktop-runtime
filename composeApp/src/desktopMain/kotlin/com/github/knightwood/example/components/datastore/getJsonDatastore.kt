package com.github.knightwood.example.components.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * 另datastore使用json文件存储偏好值
 * ```
 * @Serializable
 * data class XSettings(
 *     val skikoRenderApi: SkikoRenderApi = SkikoRenderApi.SOFTWARE,
 *     val singleInstance: Boolean = true,
 *     val closeAppDirectly : Boolean = true,
 * )
 * //使用koin提供单例（非必需）
 * single<DataStore<XSettings>>(named("xSettingsDS")) {
 *             getJsonDatastore(
 *                 defaults = XSettingsProvider.defaultValue(),
 *                 produceFile = {  FilePathProvider.publicConfigDir.resolve("xSettings.json").toFile() },
 *                 corruptionHandler= ReplaceFileCorruptionHandler {
 *                     SwingUtilities.invokeLater {
 *                         val jFrame = JFrame("提示")
 *                         jFrame.iconImage = null
 *                         jFrame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
 *                         buildPreferencesErrorDialog(jFrame)
 *                     }
 *                     XSettingsProvider.defaultValue()
 *                 }
 *             )
 *         }
 * ```
 */
inline fun <reified T> getJsonDatastore(
    defaults: T,
    json: Json = Json(Json.Default) {
        this.prettyPrint = true
    },
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    migrations: List<DataMigration<T>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    noinline produceFile: () -> File,
): DataStore<T> {
    return DataStoreFactory.create(
        serializer = object : Serializer<T> {
            override val defaultValue: T
                get() = defaults

            override suspend fun readFrom(input: InputStream): T {
                return runCatching { json.decodeFromStream<T>(input) }.getOrDefault(defaults)
            }

            override suspend fun writeTo(t: T, output: OutputStream) {
                json.encodeToStream(t, output)
            }
        },
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope,
        produceFile = produceFile,
    )
}
