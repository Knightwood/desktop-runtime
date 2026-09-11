plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
//    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("publish")
}

kotlin {
    jvm()
    jvmToolchain(17)
    sourceSets {
        commonMain.dependencies {
            api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

            //kotlin
            implementation(libs.kotlin.coroutines.core)
//            runtimeOnly(libs.kotlin.coroutines.slf4j)//https://github.com/Kotlin/kotlinx.coroutines/blob/master/integration/kotlinx-coroutines-slf4j/README.md

            //compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.savestate)
            implementation(libs.androidx.savestate)
//            implementation(libs.kotlin.serialization)
            // SLF4J
            implementation("org.slf4j:slf4j-api:2.0.15")
            implementation("com.github.knightwood:slf4j-api-kotlin:0.0.7")


            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)  // No version needed
        }

        jvmMain.dependencies {
            implementation(libs.core.bundle)
            implementation(libs.okio)
            implementation(project(":toolkit:spi"))
            implementation(project(":framework:common"))
            implementation(libs.kotlin.coroutines.swing)
            implementation(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material")
            }
            // logback-classic 1.3.15是最后的java 8 版本，后续版本要求java 11
            implementation("ch.qos.logback:logback-classic:1.5.12")
//            implementation(libs.jna)
//            implementation(libs.jna.platform)
//            implementation(libs.jnativehook)
            //spi
            implementation(libs.autoService.annoations)
        }

        configurations {
            all {
                exclude(group = "io.opentelemetry")
                exclude(group = "io.opentelemetry.semconv")
                exclude(group = "net.java.dev.jna", module = "jna-jpms")
                exclude(group = "net.java.dev.jna", module = "jna-platform-jpms")
                exclude(group = "org.seleniumhq.selenium", module = "selenium-firefox-driver")
                exclude(group = "org.seleniumhq.selenium", module = "selenium-edge-driver")
                exclude(group = "org.seleniumhq.selenium", module = "selenium-ie-driver")
                exclude(group = "org.seleniumhq.selenium", module = "selenium-manager")
            }
        }
    }
}

dependencies {
    add("kspJvm", libs.autoService.ksp)
}
