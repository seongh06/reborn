import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.reborn.library)
    alias(libs.plugins.buildconfig)
}

setNamespace("core.network")

fun getProperty(key: String): String {
    val localValue = gradleLocalProperties(rootDir, providers).getProperty(key)
    if (localValue != null) return localValue

    val envValue = System.getenv(key)
    if (envValue != null) return envValue

    error("\n\n[에러] '$key' 설정이 누락되었습니다! \nlocal.properties 또는 CI 환경변수에 해당 키를 추가해주세요.\n")
}


buildConfig {
    packageName("com.reborn.core.network")

    val isDebug = project.gradle.startParameter.taskNames.any { it.contains("debug", ignoreCase = true) }
    buildConfigField("boolean", "DEBUG", isDebug.toString())

    buildConfigField("String", "BASE_URL", "\"${getProperty("BASE_URL")}\"")

    buildConfigField("String", "KAKAO_APP_KEY", "\"${getProperty("KAKAO_APP_KEY")}\"")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(projects.core.domain)
            implementation(projects.core.common)
            //implementation(projects.core.notification)
            implementation(projects.core.datastore)
        }
        iosMain.dependencies {
            //Todo: iOS 카카오 로그인 구현하기
            implementation(libs.ktor.client.darwin)
        }

        androidMain.dependencies {
            implementation(libs.kakao.sdk.user)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }
    }
}