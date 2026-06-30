import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.reborn.application)
    alias(libs.plugins.oss)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.projectDir.resolve("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use(localProperties::load)
}

val kakaoAppKey = (
        localProperties.getProperty("KAKAO_APP_KEY")
            ?: providers.gradleProperty("KAKAO_APP_KEY").orNull
            ?: System.getenv("KAKAO_APP_KEY")
        ).orEmpty()
    .trim()
    .takeIf { it.isNotEmpty() }
    ?: error("KAKAO_APP_KEY를 local.properties, Gradle property, 또는 환경변수로 설정해주세요.")

kotlin {
    androidTarget()
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(project(":core:network"))
        }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kakao.sdk.user)
            implementation(libs.play.services.oss.licenses)
        }

        commonMain.dependencies {

            implementation(libs.androidx.navigation.compose)

            implementation(projects.core.common)
            implementation(projects.core.data)
            //implementation(projects.core.datastore)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.model)
            implementation(projects.core.navigation)
            //implementation(projects.core.notification)
            implementation(projects.core.network)
            implementation(projects.core.ui)

            implementation(projects.feature.admin.adjust)
            implementation(projects.feature.admin.data)
            implementation(projects.feature.admin.feedback)
            implementation(projects.feature.admin.home)
            implementation(projects.feature.admin.setting)
            implementation(projects.feature.aerometer)
            implementation(projects.feature.intro)

            implementation(libs.precompose)
            implementation(libs.precompose.viewmodel)

            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.core)
            implementation(libs.ktor.client.core)

            api(project(":core:network"))
        }

        commonTest.dependencies {

        }

        iosMain.dependencies {

        }
    }
}

android {
    namespace = "com.reborn"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "com.reborn"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["KAKAO_APP_KEY"] = kakaoAppKey

        buildConfigField("String", "KAKAO_APP_KEY", "\"$kakaoAppKey\"")

    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
}

