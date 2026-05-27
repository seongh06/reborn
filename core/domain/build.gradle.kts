import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.library)
}

setNamespace("core.domain")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
        }
    }
}