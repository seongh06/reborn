import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.feature)
}

setNamespace("feature.admin.setting")

kotlin {
    sourceSets {
        commonMain.dependencies {

        }

        val androidMain by getting {
            dependencies {
                implementation(libs.play.services.oss.licenses)
            }
        }
    }
}