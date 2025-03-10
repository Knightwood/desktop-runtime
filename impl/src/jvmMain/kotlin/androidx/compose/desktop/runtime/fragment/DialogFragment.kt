package androidx.compose.desktop.runtime.fragment


/**
 * 包装DialogWindow
 *
 * ```
 * var isDialogOpen by remember { mutableStateOf(false) }
 *
 * Button(onClick = { isDialogOpen = true }) {
 *     Text(text = "Open dialog")
 * }
 *
 * if (isDialogOpen) {
 *     DialogWindow(
 *         onCloseRequest = { isDialogOpen = false },
 *         state = rememberDialogState(position = WindowPosition(Alignment.Center))
 *     ) {
 *         // Content of the window
 *     }
 * }
 * ```
 */
abstract class DialogFragment : Fragment() {


}
