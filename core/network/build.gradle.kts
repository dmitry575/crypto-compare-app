import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val debugBaseUrl = project.requireDebugProperty("DEBUG_BASE_URL", "http://example_ip:port")
val releaseBaseUrl = project.requireReleaseProperty("RELEASE_BASE_URL", "http://example_ip:port")

fun Project.requireReleaseProperty(
    name: String,
    fallback: String,
): String {
    val releaseRequested =
        gradle.startParameter.taskNames.any { taskName ->
            taskName.contains("release", ignoreCase = true)
        }

    val value = providers.gradleProperty(name).orNull

    if (releaseRequested && value.isNullOrBlank()) {
        throw GradleException(
            "Missing required Gradle property '$name' for release build. " +
                "Provide it via gradle.properties or CI environment.",
        )
    }

    return value ?: fallback
}

fun Project.requireDebugProperty(
    name: String,
    fallback: String,
): String {
    val debugRequested =
        gradle.startParameter.taskNames.any { taskName ->
            taskName.contains("debug", ignoreCase = true)
        }

    val value = providers.gradleProperty(name).orNull

    if (debugRequested && value.isNullOrBlank()) {
        throw GradleException(
            "Missing required Gradle property '$name' for debug build. " +
                "Provide it via gradle.properties or CI environment.",
        )
    }

    return value ?: fallback
}
android {
    namespace = "com.cryptocompare.network"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField(
                "String",
                "BASE_URL",
                "\"$releaseBaseUrl\"",
            )
        }

        debug {
            buildConfigField(
                "String",
                "BASE_URL",
                "\"$debugBaseUrl\"",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests {
            // WebSocketClient пишет в android.util.Log, которого нет в JVM-тестах
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:helpers"))

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
}
