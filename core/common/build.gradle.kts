import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.reborn.library)
    alias(libs.plugins.reborn.compose)
    alias(libs.plugins.buildconfig)
}

setNamespace("core.common")

fun getProperty(key: String): String {
    val localValue = gradleLocalProperties(rootDir, providers).getProperty(key)
    if (localValue != null) return localValue

    val envValue = System.getenv(key)
    if (envValue != null) return envValue

    error("\n\n[에러] '$key' 설정이 누락되었습니다! \nlocal.properties 또는 CI 환경변수에 해당 키를 추가해주세요.\n")
}

buildConfig {
    packageName("com.reborn.core.common")

    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${getProperty("GOOGLE_WEB_CLIENT_ID")}\"")
}

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
            implementation(libs.kakao.sdk.user)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.google.id)
        }
        iosMain.dependencies {

        }
    }
}