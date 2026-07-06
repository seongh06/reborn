import com.reborn.configureKtor
import com.reborn.configureKotlinMultiplatform
import com.reborn.configureCoroutineKmp
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

configureKotlinMultiplatform()
configureKtor()
configureCoroutineKmp()

val autoNamespace = "com.reborn.${project.path.removePrefix(":").replace(":", ".")}"

extensions.configure<KotlinMultiplatformExtension> {
    android {
        compileSdk = 37
        minSdk = 28
        namespace = autoNamespace
    }
}
