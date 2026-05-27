import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.feature)
}

setNamespace("feature.admin.feedback")

kotlin {
    sourceSets {
        commonMain.dependencies {

        }
    }
}