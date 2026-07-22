import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.junit)
    testImplementation(libs.junit)
    api("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
