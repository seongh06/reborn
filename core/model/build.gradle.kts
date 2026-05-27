import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.library)
    alias(libs.plugins.reborn.compose)
}

setNamespace("core.model")

kotlin {
    sourceSets {
        commonMain.dependencies {

        }
    }
}