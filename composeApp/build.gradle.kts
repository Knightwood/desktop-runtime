import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs){
                exclude("org.jetbrains.compose.material")
            }
            implementation(libs.kotlin.coroutines.swing)
            implementation(project(":impl"))
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.github.knightwood.example.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.github.knightwood.example"
            packageVersion = "1.0.0"
        }
    }
}
