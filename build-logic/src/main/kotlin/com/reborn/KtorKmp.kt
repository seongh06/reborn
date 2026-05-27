package com.reborn

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKtor() {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.apply {
            getByName("commonMain").dependencies {
                implementation(libs.findLibrary("ktor-client-auth").get())
                implementation(libs.findLibrary("ktor-client-core").get())
                implementation(libs.findLibrary("ktor-client-content-negotiation").get())
                implementation(libs.findLibrary("ktor-client-logging").get())
                implementation(libs.findLibrary("ktor-serialization-kotlinx-json").get())
            }

            findByName("androidMain")?.dependencies {
                implementation(libs.findLibrary("ktor-client-okhttp").get())
            }

            findByName("iosMain")?.dependencies {
                implementation(libs.findLibrary("ktor-client-darwin").get())
            }
        }
    }
}