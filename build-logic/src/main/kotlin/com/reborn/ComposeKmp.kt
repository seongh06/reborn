package com.reborn

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

internal fun Project.configureComposeKmp() {
    if (!pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
    }

    with(plugins) {
        apply("org.jetbrains.compose")
        apply("org.jetbrains.kotlin.plugin.compose")
    }

    val isRelease = project.gradle.startParameter.taskNames.any {
        it.contains("Release", ignoreCase = true)
    }

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.apply {
            getByName("commonMain").dependencies {
                implementation(libs.findLibrary("compose-runtime").get())
                implementation(libs.findLibrary("compose-foundation").get())
                implementation(libs.findLibrary("compose-material3").get())
                implementation(libs.findLibrary("compose-ui").get())
                implementation(libs.findLibrary("compose-uiToolingPreview").get())
                implementation(libs.findLibrary("compose-components-resources").get())
            }

            findByName("androidMain")?.dependencies {
                implementation(libs.findLibrary("compose-ui-tooling").get())
            }
        }
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        includeSourceInformation.set(!isRelease)
    }
}