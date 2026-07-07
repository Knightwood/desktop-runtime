package com.github.knightwood.example.components.render

import org.jetbrains.skiko.FrameBuffering
import org.jetbrains.skiko.SkikoProperties

enum class SkikoRenderApi {
    UNKNOWN,

    /**
     * 此选择会在内部经过处理得到[SOFTWARE_COMPAT]或[SOFTWARE_FAST]
     */
    SOFTWARE,

    /**
     * Fast software rendering that works in 95% of cases. If it doesn't work,
     * Skiko will fallback to SOFTWARE_COMPAT.
     *
     * For example, on JVM it doesn't work if the system has 16-bit color.
     */
    SOFTWARE_FAST,

    /**
     * Slower software rendering that works in all cases. On JVM uses
     * [java.awt.BufferedImage] as intermediate buffer.
     */
    SOFTWARE_COMPAT,

    OPENGL,
    DIRECT3D,
    VULKAN,
    METAL,
    WEBGL;

    companion object {
        fun parse(name: String): SkikoRenderApi {
            return when (name) {
                "SOFTWARE" -> SkikoRenderApi.SOFTWARE
                "SOFTWARE_FAST" -> SkikoRenderApi.SOFTWARE
                "SOFTWARE_COMPAT" -> SkikoRenderApi.SOFTWARE
                "OPENGL" -> SkikoRenderApi.OPENGL
                "DIRECT3D" -> SkikoRenderApi.DIRECT3D
                "VULKAN" -> SkikoRenderApi.VULKAN
                "METAL" -> SkikoRenderApi.METAL
                "WEBGL" -> SkikoRenderApi.WEBGL
                else -> {
                    SkikoRenderApi.UNKNOWN
                }
            }
        }
    }
}

/**
 * [org.jetbrains.skiko.Setup]
 * [org.jetbrains.skiko.SkiaLayer]
 * [org.jetbrains.skiko.SkiaLayerProperties]
 * [org.jetbrains.skiko.SkikoProperties]
 */
object SkikoPropertiesHelper {
    /**
     * 如果在windows机器上发现透明背景窗口无法显示，可以使用opengl渲染 如果渲染非常慢，非常卡顿，可以切换到software模式
     */
    fun changeRenderApi(renderApi: SkikoRenderApi) {
        System.setProperty(Key.RENDER_API, renderApi.name)
    }

    fun changeRenderApi(renderApi: String) {
        System.setProperty(Key.RENDER_API, renderApi)
    }

    /**
     * [SkikoProperties]
     */
    object Key {
        /**
         * skiko library位置
         */
        const val skiko_library_path = "skiko.library.path"

        /**
         * The path where to store data files. It is used for extracting the Skiko
         * binaries (if libraryPath isn't null) and logging.
         *
         * 默认路径："${getProperty("user.home")}/.skiko/"
         */
        const val skiko_data_path = "skiko.data.path"

        /**
         * DOUBLE ->[FrameBuffering.DOUBLE] TRIPLE -> [FrameBuffering.TRIPLE] else
         * -> [FrameBuffering.DEFAULT]
         */
        const val skiko_buffering = "skiko.buffering"

        /**
         * vsyncEnabled 默认为true
         */
        const val vsync_enabled = "skiko.vsync.enabled"

        /**
         * 默认为true
         */
        const val macOSWaitForPreviousFrameVsyncOnRedrawImmediately =
            "skiko.rendering.macos.waitForPreviousFrameVsyncOnRedrawImmediately"

        /**
         * 默认为false
         */
        const val windowsWaitForVsyncOnRedrawImmediately =
            "skiko.rendering.windows.waitForFrameVsyncOnRedrawImmediately"

        /**
         * 默认为false
         */
        const val linuxWaitForVsyncOnRedrawImmediately = "skiko.rendering.linux.waitForFrameVsyncOnRedrawImmediately"

        /**
         * 默认为true
         */
        const val vsyncFramelimitFallbackEnabled = "skiko.vsync.framelimit.fallback.enabled"

        /**
         * [SkikoRenderApi]
         */
        const val RENDER_API = "skiko.renderApi"

        const val gpu_priority = "skiko.gpu.priority"
        const val metal_gpu_priority = "skiko.metal.gpu.priority"
        const val dx_gpu_priority = "skiko.directx.gpu.priority"

        /**
         * 默认为false
         */
        const val macos_opengl_enable = "skiko.macos.opengl.enabled"
    }
}
