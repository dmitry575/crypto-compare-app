import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

// без явной совместимости compileJava берёт таргет установленного JDK и падает
// с «Inconsistent JVM Target Compatibility» против compileKotlin
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(libs.junit)
    testImplementation(libs.junit)
    api(libs.coroutines.test)
}
