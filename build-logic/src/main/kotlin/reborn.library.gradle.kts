import com.reborn.configureKtor
import com.reborn.configureKotlin
import com.reborn.configureKotlinMultiplatform
import com.reborn.configureCoroutineKmp

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

configureKotlinMultiplatform()
configureKotlin()
configureKtor()
configureCoroutineKmp()