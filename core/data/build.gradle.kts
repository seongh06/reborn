import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.reborn.library)
}

setNamespace("core.data")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
            implementation(project(":core:model"))
            implementation(project(":core:datastore"))
            implementation(project(":core:common"))
            api(project(":core:domain"))
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            implementation(libs.koin.core)
        }
    }
}