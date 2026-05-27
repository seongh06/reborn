package com.reborn

import com.android.build.api.dsl.LibraryExtension
import gradle.kotlin.dsl.accessors._659f28ada5b3cbfd2719111534f74c17.androidLibrary
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKotlin() {
    extensions.configure<KotlinMultiplatformExtension> {
        androidLibrary {
            compileSdk = 36
            minSdk = 28
        }
    }
}

internal fun Project.configureKotlinMultiplatform() {
    extensions.configure<KotlinMultiplatformExtension> {

        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { target ->
            target.binaries.framework {
                baseName = project.name
                isStatic = true
            }
        }

        sourceSets.all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
            }
        }
    }
}