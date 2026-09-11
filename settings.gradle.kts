rootProject.name = "desktop-activity"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
includeBuild("./build-logic")

pluginManagement {
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.aliyun.com/repository/public/")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://www.jitpack.io")
    }
}


dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal()
        maven("https://www.jitpack.io")
        maven("https://maven.aliyun.com/repository/public/")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("./build-logic/repo")
    }
    versionCatalogs {
        create("jvms") {
            from(files("./gradle/jvm.versions.toml"))
        }
    }
}

include(":composeApp")
include(":framework:common")
include(":framework:compose")
include(":framework:swing")
include(":toolkit:spi")
include(":toolkit:linux")
include(":toolkit:win")
include(":toolkit:mac")
include(":compose-tray")
