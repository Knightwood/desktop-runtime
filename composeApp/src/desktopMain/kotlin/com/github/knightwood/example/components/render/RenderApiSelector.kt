package com.github.knightwood.example.components.render

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.knightwood.example.components.expand.ExpandState
import com.github.knightwood.example.components.expand.MenuLabelText
import com.github.knightwood.example.components.expand.rememberExpandState
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkikoProperties

private val renderApis = arrayOf(
    SkikoRenderApi.SOFTWARE,
    SkikoRenderApi.OPENGL,
    SkikoRenderApi.VULKAN,
    SkikoRenderApi.DIRECT3D,
)

private val highLevelRenderApis = arrayOf(
    SkikoRenderApi.VULKAN,
    SkikoRenderApi.DIRECT3D,
)

/**
 * 渲染api只能在启动compose之前设置，且运行过程中切换渲染api也不会生效
 * 因此不必将当前渲染api的判断做成可重组的。
 */
val SkikoProperties.isHighRenderApi by lazy {
    SkikoProperties.renderApi.isHighLevel()
}

fun GraphicsApi.isHighLevel(): Boolean {
    if (this.name in highLevelRenderApis.map { renderApi -> renderApi.name }) return true
    return false
}


@Composable
fun RenderApiSelector(
    status: ExpandState = rememberExpandState(),
    enabled: Boolean = true,
    selected: SkikoRenderApi,
    onChanged: (SkikoRenderApi) -> Unit,
) {
    Box {
        MenuLabelText(
            enabled = enabled,
            modifier = Modifier.align(Alignment.Center),
            onClick = { status.show() },
            text = selected.name
        )
        DropdownMenu(expanded = status.isExpanded, onDismissRequest = status::dismiss) {
            repeat(renderApis.size) {
                val api = renderApis[it]
                DropdownMenuItem(
                    text = { Text(api.name) },
                    leadingIcon = {
                        if (api == selected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        } else {

                        }
                    },
                    onClick = {
                        onChanged(api)
                        status.dismiss()
                    }
                )
            }
        }
    }
}
