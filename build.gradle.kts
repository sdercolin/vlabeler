import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.6.21"
    id("org.jetbrains.compose")
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
}

version = "1.0.0-alpha7"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
                implementation("com.github.psambit9791:jdsp:1.0.0")
                implementation("org.python:jython-standalone:2.7.2")
                implementation("org.graalvm.js:js:22.1.0")
                implementation("org.apache.tika:tika-parser-text-module:2.4.1")

                val lwjglVersion = "3.3.1"
                listOf("lwjgl", "lwjgl-nfd").forEach { lwjglDep ->
                    implementation("org.lwjgl:$lwjglDep:$lwjglVersion")
                    listOf("natives-windows", "natives-windows-x86", "natives-windows-arm64").forEach { native ->
                        runtimeOnly("org.lwjgl:$lwjglDep:$lwjglVersion:$native")
                    }
                }
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.sdercolin.vlabeler.MainKt"
        jvmArgs("-Xmx2G")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "vLabeler"
            packageVersion = (version as String).split("-").first()
            copyright = "© 2022 sdercolin. All rights reserved."
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            modules("java.sql", "jdk.charsets", "jdk.unsupported", "jdk.accessibility")
        }
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("0.45.2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}
