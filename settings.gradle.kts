pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io" ) }
    }
    resolutionStrategy {
        eachPlugin {
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url = uri("https://jitpack.io" ) }
    }
}

rootProject.name = "HealthGraphs"
include(":app")
