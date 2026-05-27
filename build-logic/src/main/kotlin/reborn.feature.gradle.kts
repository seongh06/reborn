import com.reborn.configureCoil
import com.reborn.libs
import gradle.kotlin.dsl.accessors._659f28ada5b3cbfd2719111534f74c17.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("reborn.library")
    id("reborn.compose")
}

extensions.configure<KotlinMultiplatformExtension> {
    androidLibrary {
        packaging {
            resources.excludes.add("META-INF/**")
        }
    }
}

extensions.configure<KotlinMultiplatformExtension> {
    sourceSets.apply {
        getByName("commonMain").dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            //implementation(project(":core:datastore"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:domain"))
            implementation(project(":core:model"))
            implementation(project(":core:navigation"))
            implementation(project(":core:network"))
            implementation(project(":core:ui"))

            implementation(libs.findLibrary("koin-compose").get())
            implementation(libs.findLibrary("koin-compose-viewmodel").get())
            implementation(libs.findLibrary("androidx-navigation-compose").get())
            implementation(libs.findLibrary("precompose-viewmodel").get())
            implementation(libs.findLibrary("precompose-koin").get())
            implementation(libs.findLibrary("compose-reorderable").get())
        }
    }
}

configureCoil()