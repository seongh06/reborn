package com.reborn

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureCoroutineKmp() {

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.apply {
            getByName("commonMain").dependencies {
                implementation(libs.findLibrary("coroutines-core").get())
            }

            findByName("androidMain")?.dependencies {
                implementation(libs.findLibrary("coroutines-android").get())
            }

            getByName("commonTest").dependencies {
                implementation(libs.findLibrary("coroutines-test").get())
            }
        }
    }
}