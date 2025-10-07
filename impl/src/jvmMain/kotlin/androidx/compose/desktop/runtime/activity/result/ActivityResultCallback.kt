package androidx.compose.desktop.runtime.activity.result



class ActivityResult(
    val resultCode: Int,
    val data: Any?
)


/**
 * activity的结果回调
 */
fun interface ActivityResultCallback {
    fun invoke(resultCode: Int, data: Any?)
}