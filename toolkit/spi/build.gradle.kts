plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
    id("publish")
}


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
    jvmToolchain(17)
    compilerOptions {
        val optIns = getOptIns().map { "-Xopt-in=$it" }
        val features = getFeatures().map { "-X$it" }
        freeCompilerArgs.set(optIns + features)
    }
}

dependencies {
    //kotlin
    implementation(libs.kotlin.coroutines.core)
//    runtimeOnly(libs.kotlin.coroutines.slf4j)//https://github.com/Kotlin/kotlinx.coroutines/blob/master/integration/kotlinx-coroutines-slf4j/README.md


    implementation(libs.kotlin.coroutines.swing)


    implementation(libs.okio)
    // SLF4J
    implementation("org.slf4j:slf4j-api:2.0.15")
    implementation("com.github.knightwood:slf4j-api-kotlin:0.0.7")
    // logback-classic 1.3.15是最后的java 8 版本，后续版本要求java 11
    implementation("ch.qos.logback:logback-classic:1.5.12")

    //jna
    implementation(libs.jna)
    implementation(libs.jna.platform)
//    implementation(libs.jnativehook)

    //spi
    ksp(libs.autoService.ksp)
    implementation(libs.autoService.annoations)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)  // No version needed
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
