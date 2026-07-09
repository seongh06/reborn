rootProject.name = "reborn"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://devrepo.kakao.com/nexus/content/groups/public/")
    }
}

include(":composeApp")
include(":server")

include(":core:model")
include(":core:network")
include(":core:data")
include(":core:domain")
include(":core:designsystem")
include(":core:ui")
include(":core:navigation")
include(":core:common")
include(":core:datastore")
include(":core:notification")

include(":feature:intro")
include(":feature:aerometer")
include(":feature:admin:home")
include(":feature:admin:adjust")
include(":feature:admin:feedback")
include(":feature:admin:data")
include(":feature:admin:setting")
