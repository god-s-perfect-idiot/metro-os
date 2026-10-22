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

rootProject.name = "music"
include(":app")

includeBuild("../../toolkits/metro-ui-android")
includeBuild("../../toolkits/metro-system-sdk")

// Metrolist/zemer stream unlock: signature cipher, n-transform, remote player configs.
includeBuild("third_party/zemer-cipher") {
    dependencySubstitution {
        substitute(module("com.zemer:cipher")).using(project(":library"))
    }
}
