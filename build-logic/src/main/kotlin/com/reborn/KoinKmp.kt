package com.reborn

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKoin() {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.apply {
            getByName("commonMain").dependencies {
                implementation(libs.findLibrary("koin-core").get())
                implementation(libs.findLibrary("koin-compose").get())
                implementation(libs.findLibrary("koin-compose-viewmodel").get())
            }

            findByName("androidMain")?.dependencies {
                implementation(libs.findLibrary("koin-android").get())
            }
        }
    }
}