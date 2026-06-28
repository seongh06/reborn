plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.verify.detektPlugin)
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.6.11")
    compileOnly(libs.compose.compiler.gradle.plugin)
}