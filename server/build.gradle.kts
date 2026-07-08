import org.gradle.api.JavaVersion
import org.gradle.api.tasks.bundling.Jar

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinJpa)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

group = "com.reborn.server"
version = "1.0.0"

dependencies {
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.springdoc.openapi.webmvc)
    implementation(libs.jjwt.api)
    implementation(libs.aws.sdk.s3)
    implementation(libs.firebase.admin)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    runtimeOnly(libs.mysql.connector.j)
    testImplementation(libs.spring.boot.starter.test)
}

tasks.test {
    useJUnitPlatform()
}

// plain jar(-plain.jar)를 비활성화해 Dockerfile의 `COPY *.jar app.jar`가
// bootJar 산출물 하나만 매칭하도록 보장
tasks.named<Jar>("jar") {
    enabled = false
}
