import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.reborn.setNamespace

plugins {
    alias(libs.plugins.reborn.library)
    alias(libs.plugins.reborn.compose)
    alias(libs.plugins.kotlin.serialization)
}

setNamespace("core.navigation")

compose.resources {
    generateResClass = always
    publicResClass = true
    packageOfResClass = "com.reborn.core.navigation.generated.resources"
}

val packageName = "com.reborn.core.navigation.generated.resources"
val generatedJavaResDir = layout.buildDirectory.dir("generated/composeResources")

val copyNavigationComposeResources = tasks.register<Copy>("copyNavigationComposeResources") {
    dependsOn("prepareComposeResourcesTaskForCommonMain")
    from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"))
    into(generatedJavaResDir.map { it.dir("composeResources/$packageName") })
}

tasks.whenTaskAdded {
    if (name == "processAndroidMainJavaRes") {
        dependsOn(copyNavigationComposeResources)
    }
}

kotlin {
    sourceSets {
        named("androidMain") {
            resources.srcDir(generatedJavaResDir)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}