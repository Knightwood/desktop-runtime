package androidx.jvm.system.ui.tray.menu_dialog

import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.PopupPositionProviderAtPosition
import androidx.jvm.system.utils.GlobalLayoutDirection
import com.github.knightwood.slf4j.kotlin.logFor
import java.awt.Component
import java.awt.Insets
import java.awt.Toolkit
import java.awt.Window

private val logger = logFor("托盘弹窗位置计算")

@OptIn(ExperimentalComposeUiApi::class)
fun Window.moveSafe(
    position: IntOffset,
    alignment: Alignment = Alignment.BottomEnd,
) {
    val window = this
    logger.debug("窗口宽高${window.width}X${window.height}")
    val p = PopupPositionProviderAtPosition(
        positionPx = Offset.Zero,
        isRelativeToAnchor = true,
        offsetPx = Offset.Zero,
        alignment = alignment,
        windowMarginPx = 0,
    )

    val screenSize = getScreenSize()
    val insets = getScreenInsets(window)
    val offset = p.calculatePosition(
        popupContentSize = IntSize(
            window.width, window.height
        ),
        layoutDirection = GlobalLayoutDirection,
        windowSize = screenSize - insets,
        anchorBounds = IntRect(
            position.x,
            position.y,
            position.x,
            position.y,
        ),
    ) + insets
    logger.debug("最终显示位置左上角$offset")
    window.setLocation(
        offset.x,
        offset.y,
    )
}
fun getScreenInsets(component: Component): Insets {
    return runCatching {
        Toolkit.getDefaultToolkit().getScreenInsets(component.graphicsConfiguration)
    }.getOrElse {
        Insets(0, 0, 0, 0)
    }
}

private operator fun IntSize.minus(insets: Insets): IntSize {
    return IntSize(
        width - (insets.left + insets.right),
        height - (insets.top + insets.bottom)
    )
}

private operator fun IntOffset.plus(insets: Insets): IntOffset {
    return copy(
        x = x + insets.left,
        y = y + insets.top,
    )
}

//it is dp size!
private fun getScreenSize(): IntSize {
    Toolkit.getDefaultToolkit().screenSize.run {
        return IntSize(
            width, height
        )
    }
}
