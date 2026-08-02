pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "metro-volume"
include(":app")

includeBuild("../../toolkits/metro-ui-android")
includeBuild("../../toolkits/metro-system-sdk")
