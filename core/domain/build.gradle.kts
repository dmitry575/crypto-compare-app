import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cryptocompare.domain"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        minSdk = 26
    }

    testOptions {
        targetSdk = 37
        unitTests.isIncludeAndroidResources = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.androidTest.enable = false
    }
}

dependencies {
    // modules
    api(project(":core:model"))
    api(project(":core:helpers"))

    // coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.play.services)

    // paging
    api(libs.androidx.paging.common)

    // di
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
