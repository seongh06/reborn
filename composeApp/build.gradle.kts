import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.reborn.application)
    alias(libs.plugins.oss)
    alias(libs.plugins.googleServices)
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
            implementation(projects.core.datastore)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.model)
            implementation(projects.core.navigation)
            implementation(projects.core.notification)
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

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

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
    // 디버그 서명키를 리포에 고정 커밋해둠 - AGP 기본 debug.keystore는 머신마다(그리고 GitHub
    // Actions 러너마다 매 실행 시) 새로 생성되어 서명 키가 달라짐. 카카오/구글 로그인은 서명 키의
    // key hash/SHA-1을 콘솔에 등록해야 동작하는데, 키가 매번 바뀌면 CI가 빌드한 debug APK를 다른
    // 사람이 설치했을 때 로그인이 전부 실패함. 고정 키스토어로 로컬/CI가 항상 같은 키로 서명하게 함.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // 릴리즈 서명키(composeApp/release.keystore)는 debug.keystore와 달리 git에 올리지 않음 -
        // 분실 시 Play 콘솔에 이미 배포한 앱을 재배포할 수 없는 민감 키라 로컬/CI 시크릿으로만 관리.
        // 로컬 개발자는 local.properties, CI는 환경변수(RELEASE_*)로 값을 주입.
        val releaseStorePassword = (
                localProperties.getProperty("RELEASE_STORE_PASSWORD")
                    ?: providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
                    ?: System.getenv("RELEASE_STORE_PASSWORD")
                ).orEmpty()
        // 존재 여부만 trim해서 판단하고, SigningConfig에는 원문을 그대로 넘긴다 - 비밀번호 자체를
        // trim하면 공백이 비밀번호의 일부인 경우 다른 자격증명으로 서명이 조용히 성공해버린다
        // (CodeRabbit 리뷰, PR #324).
        if (releaseStorePassword.trim().isNotEmpty()) {
            create("release") {
                storeFile = file("release.keystore")
                storePassword = releaseStorePassword
                keyAlias = "reborn-release"
                keyPassword = releaseStorePassword
            }
        }
    }
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
            signingConfigs.findByName("release")?.let { signingConfig = it }
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

// release 서명이 없으면 AGP는 조용히 서명 안 된 산출물을 만들고 넘어간다(assemble 자체는
// 성공) - RELEASE_STORE_PASSWORD 미설정을 빌드 실패가 아니라 "그냥 안 된 채로 성공"으로
// 놔두면 CI/로컬에서 미서명 release APK를 못 알아채고 지나칠 수 있어 release 관련 태스크
// 실행 시점에만 명시적으로 막는다(CodeRabbit 리뷰, PR #324) - debug 빌드는 영향 없음.
tasks.matching { it.name.contains("Release") }.configureEach {
    doFirst {
        check(android.signingConfigs.findByName("release") != null) {
            "RELEASE_STORE_PASSWORD가 설정되지 않아 release 서명을 할 수 없습니다. " +
                "local.properties 또는 환경변수로 설정해주세요."
        }
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
}

