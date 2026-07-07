package com.github.knightwood.example.components.expand

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun MenuLabelText(
    modifier: Modifier = Modifier,
    text: String, enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        enabled = enabled,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minHeight = 48.dp, minWidth = 90.dp)
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyMedium.fixEnabledColor(enabled),
                text = text
            )
        }
    }
}
fun Color.applyOpacity(enabled: Boolean): Color {
    return if (enabled) this else this.copy(alpha = 0.62f)
}
fun TextStyle.fixEnabledColor(enabled: Boolean): TextStyle {
    return if (enabled) this else this.copy(
        color = this.color.applyOpacity(false)
    )
}
