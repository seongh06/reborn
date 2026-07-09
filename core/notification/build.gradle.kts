import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.library)
}

setNamespace("core.notification")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.firebase.messaging)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
        }
        iosMain.dependencies {

        }
    }
}
