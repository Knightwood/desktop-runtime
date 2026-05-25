package androidx.jvm.system.windows

/**
 * 窗口描述信息，用于调用平台native api操作窗口时使用
 *
 * ```
 * val desc = "窗口名称".asWindowDesc()
 * ```
 * @param title 操作窗口时一般只需一个窗口名称即可
 */
data class WindowDesc(
    val title: String,
)

fun String.asWindowDesc(): WindowDesc {
    return WindowDesc(this)
}
