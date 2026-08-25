package androidx.compose.desktop.runtime.activity

import androidx.core.bundle.Bundle


class ActivityResult(
    val resultCode: Int,
    val data: Bundle?
)


/**
 * activity的结果回调
 */
fun interface ActivityResultCallback {
    fun invoke(resultCode: Int, data: Any?)
}
