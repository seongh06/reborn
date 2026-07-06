import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.library)
    alias(libs.plugins.reborn.compose)
}

setNamespace("core.ui")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
            implementation(projects.core.designsystem)
            implementation(projects.core.model)
            implementation(projects.core.common)

        }
    }
}