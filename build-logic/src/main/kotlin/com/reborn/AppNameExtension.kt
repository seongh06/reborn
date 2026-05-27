package com.reborn

import org.gradle.api.Project
import com.android.build.api.dsl.CommonExtension
import gradle.kotlin.dsl.accessors._659f28ada5b3cbfd2719111534f74c17.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.setNamespace(name: String) {
    val namespaceValue = "com.reborn.$name"

    val androidExtension = extensions.findByName("android") as? CommonExtension<*, *, *, *, *, *>
    if (androidExtension != null) {
        androidExtension.namespace = namespaceValue
        return
    }

    val kmpExtension = extensions.findByType(KotlinMultiplatformExtension::class.java)
    if (kmpExtension != null) {
        kmpExtension.androidLibrary {
            namespace = namespaceValue
        }
    } else {
        logger.info("Android extension not found for project: ${project.name}. Skipping namespace setup.")
    }
}