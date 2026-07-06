import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.library)
    alias(libs.plugins.reborn.compose)
    alias(libs.plugins.kotlin.serialization)
}

setNamespace("core.navigation")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}