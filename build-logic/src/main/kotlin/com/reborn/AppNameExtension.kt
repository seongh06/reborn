package com.reborn

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension

fun Project.setNamespace(name: String) {
    val namespaceValue = "com.reborn.$name"

    // Standard Android module (com.android.application / com.android.library)
    (extensions.findByName("android") as? CommonExtension)?.namespace = namespaceValue

    // KMP module (com.android.kotlin.multiplatform.library)
    // namespace is also auto-set in reborn.library.gradle.kts; this is a best-effort backup
    // "android" accessor replaced "androidLibrary" as of AGP 9.x
    extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kmp ->
        (kmp.extensions.findByName("android") as? KotlinMultiplatformAndroidLibraryExtension)
            ?.apply { namespace = namespaceValue }
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
