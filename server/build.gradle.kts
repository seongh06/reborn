import org.gradle.api.JavaVersion

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

group = "com.reborn.server"
version = "1.0.0"

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.springdoc.openapi.webmvc)
    testImplementation(libs.spring.boot.starter.test)
}
