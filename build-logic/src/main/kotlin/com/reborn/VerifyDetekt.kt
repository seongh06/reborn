package com.reborn

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureVerifyDetekt() {
    with(pluginManager) {
        apply("io.gitlab.arturbosch.detekt")
    }
    dependencies {
        add("detektPlugins", libs.findLibrary("verify.detektFormatting").get())
    }
}