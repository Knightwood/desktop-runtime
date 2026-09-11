@file:OptIn(ExperimentalResourceApi::class)

package androidx.jvm.system.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.decodeToImageVector
import org.jetbrains.compose.resources.decodeToSvgPainter

/**
 * 新的资源库使用Res.drawable.xxx形式，
 * 如果还希望直接读取resources文件夹下的内容，使用这里的代码即可
 * To read java resources by path as Painters
 * https://github.com/JetBrains/compose-multiplatform-core/pull/1457
 * 将文件放在resources或它的子目录下，传入相对路径
 */
@Composable
fun painterResource(
    resourcePath: String
): Painter = when (resourcePath.substringAfterLast(".")) {
    "svg" -> rememberSvgResource(resourcePath)
    "xml" -> rememberVectorXmlResource(resourcePath)
    else -> rememberBitmapResource(resourcePath)
}

@Composable
internal fun rememberBitmapResource(path: String): Painter {
    return remember(path) { BitmapPainter(readResourceBytes(path).decodeToImageBitmap()) }
}

@Composable
internal fun rememberVectorXmlResource(path: String): Painter {
    val density = LocalDensity.current
    val imageVector = remember(density, path) { readResourceBytes(path).decodeToImageVector(density) }
    return rememberVectorPainter(imageVector)
}

@Composable
internal fun rememberSvgResource(path: String): Painter {
    val density = LocalDensity.current
    return remember(density, path) { readResourceBytes(path).decodeToSvgPainter(density) }
}

private object ResourceLoader

private fun readResourceBytes(resourcePath: String) =
    ResourceLoader.javaClass.classLoader.getResourceAsStream(resourcePath).readAllBytes()
