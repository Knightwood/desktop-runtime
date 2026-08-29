package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Stable
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.FrameWindowScope

@Stable
interface ActivityWindowScope: FrameWindowScope {
}
@Stable
interface ComponentDialogWindowScope: DialogWindowScope {
}
