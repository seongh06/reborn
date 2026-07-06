import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.library)
    alias(libs.plugins.reborn.compose)
}

setNamespace("core.common")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.mlkit.face.detection)
            implementation(libs.androidx.lifecycle.process)
        }
        iosMain.dependencies {

        }
    }
}