package com.reborn

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import com.android.build.api.dsl.CommonExtension
import gradle.kotlin.dsl.accessors._659f28ada5b3cbfd2719111534f74c17.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension

fun Project.setNamespace(name: String) {
    val namespaceValue = "com.reborn.$name"

    (extensions.findByName("android") as? CommonExtension<*, *, *, *, *, *>)?.namespace = namespaceValue

    extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kmp ->
        try {
            kmp.androidLibrary {
                namespace = namespaceValue
            }
        } catch (e: Exception) {

        }
    }

    val composeExtension = extensions.findByType(ComposeExtension::class.java)
    val resourcesExtension = (composeExtension as? ExtensionAware)
        ?.extensions
        ?.findByType(ResourcesExtension::class.java)

    if (resourcesExtension != null) {
        resourcesExtension.apply {
            publicResClass = true
            packageOfResClass = namespaceValue
            generateResClass = ResourcesExtension.ResourceClassGeneration.Always
        }
    } else {
        logger.info("ResourcesExtension not found for project: ${project.name}")
    }
}