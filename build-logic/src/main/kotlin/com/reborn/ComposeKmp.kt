package com.reborn

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

internal fun Project.configureComposeKmp() {
    if (!pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
    }

    with(plugins) {
        apply("org.jetbrains.compose")
        apply("org.jetbrains.kotlin.plugin.compose")
    }

    val isRelease = project.gradle.startParameter.taskNames.any {
        it.contains("Release", ignoreCase = true)
    }

    val generatedComposeResourcesDir = layout.buildDirectory.dir("generated/composeResources")

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.apply {
            getByName("commonMain").dependencies {
                implementation(libs.findLibrary("compose-runtime").get())
                implementation(libs.findLibrary("compose-foundation").get())
                implementation(libs.findLibrary("compose-material3").get())
                implementation(libs.findLibrary("compose-ui").get())
                implementation(libs.findLibrary("compose-uiToolingPreview").get())
                implementation(libs.findLibrary("compose-components-resources").get())
            }

            findByName("androidMain")?.dependencies {
                implementation(libs.findLibrary("compose-ui-tooling").get())
            }

            matching { it.name == "androidMain" }.configureEach {
                resources.srcDir(generatedComposeResourcesDir)
            }
        }
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        includeSourceInformation.set(!isRelease)
    }

    configureComposeResourcesAndroidAssetMerge(generatedComposeResourcesDir)
}

// `com.android.kotlin.multiplatform.library` (used by KMP library modules) doesn't merge
// composeResources into the final Android assets on its own the way an application module
// does: prepared resources (src/commonMain/composeResources) sit in the build dir but never
// reach the merged APK assets, so painterResource()/Font() throw MissingResourceException at
// runtime even though Res.kt compiles fine. Manually copy them into an androidMain resources
// dir, namespaced by the module's actual packageOfResClass, so AGP's resource merging picks
// them up — mirrors what ComposeResourcesKt's CopyResourcesToAndroidAssetsTask does for apps.
private fun Project.configureComposeResourcesAndroidAssetMerge(
    generatedComposeResourcesDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>
) {
    val packageNameProvider = provider {
        val composeExtension = extensions.findByType(ComposeExtension::class.java)
        (composeExtension as? ExtensionAware)
            ?.extensions
            ?.findByType(ResourcesExtension::class.java)
            ?.packageOfResClass
            ?.takeIf { it.isNotBlank() }
    }

    val copyComposeResourcesToAndroidAssets = tasks.register<Copy>("copyComposeResourcesToAndroidAssets") {
        dependsOn("prepareComposeResourcesTaskForCommonMain")
        from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"))
        into(packageNameProvider.flatMap { packageName ->
            generatedComposeResourcesDir.map { it.dir("composeResources/$packageName") }
        })
        onlyIf { packageNameProvider.orNull != null }
    }

    tasks.whenTaskAdded {
        if (name == "processAndroidMainJavaRes") {
            dependsOn(copyComposeResourcesToAndroidAssets)
        }
    }
}