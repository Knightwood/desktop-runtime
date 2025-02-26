package androidx.compose.desktop.runtime.system.utils

import okio.Path

val Path.fileNameRemoveExtension: String
    get() {
        val fileName = name

        val index = fileName.lastIndexOf('.')

        return if (index != -1) {
            fileName.substring(0, index)
        } else {
            fileName
        }
    }

val Path.extension: String
    get() {
        val fileName = name

        val index = fileName.lastIndexOf('.')

        return if (index != -1 && index != fileName.length - 1) {
            fileName.substring(index + 1)
        } else {
            ""
        }
    }

val Path.noOptionParent: Path
    get() {
        return this.parent ?: this
    }
