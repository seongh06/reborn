package com.reborn

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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
