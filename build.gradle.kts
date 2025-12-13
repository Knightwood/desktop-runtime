plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false

}

fun org.gradle.api.Project.publishing(configure: Action<org.gradle.api.publish.PublishingExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("publishing", configure)

subprojects {
    afterEvaluate {
        if (project.plugins.hasPlugin("com.vanniktech.maven.publish")
            || project.plugins.hasPlugin("org.gradle.maven-publish")
        ) {
            val myMavenName = project.findProperty("my.maven.name") as String? ?: "MyLocalMaven"
            val myMavenUrl = project.findProperty("my.maven.url") as String? ?: "D:\\maven"
            publishing {
                repositories {
                    maven {
                        name = myMavenName
                        url = uri(myMavenUrl)
                    }
                }
            }
        }
    }
}
