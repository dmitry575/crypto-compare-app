import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

val debugWsBaseUrl = project.requireDebugProperty("DEBUG_WS_BASE_URL", "ws://example_ip:port")

val releaseWsBaseUrl = project.requireReleaseProperty("RELEASE_WS_BASE_URL", "ws://example_ip:port")

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
    namespace = "com.cryptocompare.data"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        // MigrationTestHelper читает экспортированные схемы из ассетов теста
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
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
                "WS_BASE_URL",
                "\"$releaseWsBaseUrl\"",
            )
        }

        debug {
            buildConfigField(
                "String",
                "WS_BASE_URL",
                "\"$debugWsBaseUrl\"",
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
            // конструкторы исключений Firebase дёргают android.text.TextUtils
            isReturnDefaultValues = true
        }
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    // modules
    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:helpers"))

    // coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.play.services)

    // di
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.firestore)

    // local storage
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)

    // tests
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.mockk.android)

    // миграции проверяются только на устройстве: нужен настоящий SQLite
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
}
