import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
}

group = "love.forte.ktorm-extra"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.ktorm:ktorm-core:3.5.0")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-testng"))
    testImplementation("com.h2database:h2:2.1.212")
    testImplementation("org.ktorm:ktorm-core:3.5.0")
}

tasks.test {
    useTestNG()
}


tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions {
        jvmTarget = "1.8"
        allWarningsAsErrors = true
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xexplicit-api=strict",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xjvm-default=enable"
        )
    }
}