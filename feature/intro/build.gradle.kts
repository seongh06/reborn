import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.feature)
}

setNamespace("feature.intro")

kotlin {
    sourceSets {
        commonMain.dependencies {

        }
    }
}