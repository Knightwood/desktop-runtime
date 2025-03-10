import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.ksp)
}

val groupId = "com.github.knightwood"
val artifactId = "jvm-system-mac"
val githubUrl="https://github.com/Knightwood/desktop-runtime"
group = groupId
version = "1.0.0"

fun getOptIns() = setOf(
    "com.russhwolf.settings.ExperimentalSettingsApi",
    "com.arkivanov.decompose.ExperimentalDecomposeApi",
    "androidx.compose.animation.ExperimentalAnimationApi",
    "androidx.compose.foundation.ExperimentalFoundationApi",
    "androidx.compose.ui.ExperimentalComposeUiApi",
)
fun getFeatures() = setOf(
    "context-receivers"
)

kotlin {
    compilerOptions {
        val optIns = getOptIns().map { "-Xopt-in=$it" }
        val features = getFeatures().map { "-X$it" }
        freeCompilerArgs.set(optIns + features)
    }
}

dependencies {
    //kotlin
    implementation(libs.kotlin.coroutines.core)
    runtimeOnly(libs.kotlin.coroutines.slf4j)//https://github.com/Kotlin/kotlinx.coroutines/blob/master/integration/kotlinx-coroutines-slf4j/README.md

    //compose
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlin.coroutines.swing)
    implementation(compose.desktop.currentOs){
        exclude("org.jetbrains.compose.material")
    }

    implementation(libs.okio)
    // SLF4J
    implementation("org.slf4j:slf4j-api:2.0.15")
    implementation("com.github.knightwood:slf4j-api-kotlin:0.0.7")
    // logback-classic 1.3.15是最后的java 8 版本，后续版本要求java 11
    implementation("ch.qos.logback:logback-classic:1.5.12")

    //jna
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(libs.jnativehook)

    //spi
    ksp(libs.autoService.ksp)
    implementation(libs.autoService.annoations)
    implementation(project(":system-impl:spi"))
//    implementation("com.dorkbox:SystemTray:4.4")

}

configurations.implementation {
    exclude(group = "io.opentelemetry")
    exclude(group = "io.opentelemetry.semconv")
    exclude(group = "net.java.dev.jna", module = "jna-jpms")
    exclude(group = "net.java.dev.jna", module = "jna-platform-jpms")
    exclude(group = "org.seleniumhq.selenium", module = "selenium-firefox-driver")
    exclude(group = "org.seleniumhq.selenium", module = "selenium-edge-driver")
    exclude(group = "org.seleniumhq.selenium", module = "selenium-ie-driver")
    exclude(group = "org.seleniumhq.selenium", module = "selenium-manager")
}


mavenPublishing {
    publishToMavenCentral()
//    signAllPublications()
    coordinates(group.toString(), artifactId, version.toString())

    pom {
        name = artifactId
        description = "android like desktop runtime"
        inceptionYear = "2024"
        url = "https://github.com/kotlin/multiplatform-library-template/"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "knightwood"
                name = "knightwood"
                email = "33772264+Knightwood@users.noreply.github.com"
            }
        }
        scm {
            url = githubUrl
        }
    }
}
