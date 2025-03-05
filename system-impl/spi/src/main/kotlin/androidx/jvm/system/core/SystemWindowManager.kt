package androidx.jvm.system.core

import java.util.*


fun getSystemWindowManager(): SystemWindowManager? {
    return ServiceLoader.load(SystemWindowManager::class.java).firstOrNull()
}

interface SystemWindowManager {

}
