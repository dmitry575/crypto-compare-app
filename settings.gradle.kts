pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "crypto-compare"
include(":app")
include(":core:domain")
include(":core:model")
include(":core:network")
include(":core:data")
include(":core:ui")
include(":feature:pairs")
include(":core:testing")
include(":feature:auth")
include(":feature:profile")
include(":core:helpers")
