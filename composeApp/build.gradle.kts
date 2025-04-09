import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

enum class SystemOs {
    MacOS, Windows, Linux,
}

val currentOS: SystemOs by lazy {
    val os = System.getProperty("os.name")
    when {
        os.equals("Mac OS X", ignoreCase = true)
                || os.contains("mac")
                || os.contains("darwin") -> SystemOs.MacOS

        os.startsWith("Win", ignoreCase = true) -> SystemOs.Windows
        os.startsWith("Linux", ignoreCase = true) -> SystemOs.Linux
        else -> error("Unknown OS name: $os")
    }
}


kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(libs.okio)
            implementation(libs.kotlin.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // SLF4J
            implementation("org.slf4j:slf4j-api:2.0.15")
            implementation("com.github.knightwood:slf4j-api-kotlin:0.0.7")
            //spi
            implementation(libs.autoService.annoations)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs){
                exclude("org.jetbrains.compose.material")
            }
            implementation(libs.kotlin.coroutines.swing)
            implementation(project(":impl"))
            implementation(project(":system-impl:spi"))
            when (currentOS) {
                SystemOs.Linux -> {
                    implementation(project(":system-impl:linux"))
                }
                SystemOs.Windows -> {
                    implementation(project(":system-impl:win"))
                }
                else -> {
                    implementation(project(":system-impl:mac"))
                }
            }
//            implementation("com.dorkbox:SystemTray:4.4")
        }
    }
}

//kmp还不能在上面添加ksp的依赖，上面看起来也能用implementation这样的函数，
//实际上只是域平时我们使用的implementation函数名字相同，其实现和作用域是不同的。
dependencies{
    ksp(libs.autoService.ksp) //等同于 add("ksp",libs.autoService.ksp)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "me.i18n.resources"
    generateResClass = auto
}



compose.desktop {
    application {
//        jvmArgs += listOf("-Dfile.encoding=GBK")
//        jvmArgs -= listOf("-Dfile.encoding=UTF-8")

        mainClass = "com.github.knightwood.example.MainKt"
//        javaHome = System.getenv("JAVA_HOME")
        buildTypes.release {
            proguard {
                version.set("7.5.0")
                isEnabled = false  // false to disable proguard
//                optimize = true
//                obfuscate = true //混淆。目前，room会因为这个而报错
            }
        }
        nativeDistributions {
            modules("java.instrument", "jdk.unsupported", "java.naming")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.github.knightwood.example"
            packageVersion = "1.0.0"
        }
    }
}
