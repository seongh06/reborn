import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.feature)
}

setNamespace("feature.aerometer")

kotlin {
    sourceSets {
        commonMain.dependencies {

        }
    }
}