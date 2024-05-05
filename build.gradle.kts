import com.github.jk1.license.render.JsonReportRenderer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.internal.utils.localPropertiesFile
import java.util.Properties

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    }
}

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.23"
    id("org.jetbrains.compose")
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.github.jk1.dependency-license-report") version "2.0"
}

version = project.properties["app.version"] as String

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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
                implementation(kotlin("reflect"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
                implementation(compose.desktop.currentOs)
                implementation(compose.materialIconsExtended)
                implementation(compose("org.jetbrains.compose.components:components-splitpane-desktop"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                implementation("com.github.psambit9791:jdsp:1.0.0")
                implementation("org.graalvm.js:js:22.1.0")
                implementation("org.apache.tika:tika-parser-text-module:2.4.1")
                implementation("io.ktor:ktor-client-core:2.1.0")
                implementation("io.ktor:ktor-client-apache:2.1.0")
                implementation("io.ktor:ktor-client-logging:2.1.0")
                implementation("io.ktor:ktor-client-content-negotiation:2.1.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.0")
                implementation("ch.qos.logback:logback-classic:1.2.9")
                implementation("org.zeromq:jeromq:0.5.2")
                implementation("com.segment.analytics.kotlin:core:1.9.1")
                implementation("uk.co.caprica:vlcj:4.7.0")
                implementation("cafe.adriel.bonsai:bonsai-core:1.2.0")
                implementation("org.apache.pdfbox:fontbox:2.0.24")

                val lwjglVersion = "3.3.1"
                listOf("lwjgl", "lwjgl-nfd").forEach { lwjglDep ->
                    implementation("org.lwjgl:$lwjglDep:$lwjglVersion")
                    if (System.getProperty("os.name").startsWith("win", ignoreCase = true)) {
                        listOf("natives-windows", "natives-windows-x86", "natives-windows-arm64").forEach { native ->
                            runtimeOnly("org.lwjgl:$lwjglDep:$lwjglVersion:$native")
                        }
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
        if (project.localPropertiesFile.exists()) {
            val localProperties = Properties().apply { load(project.localPropertiesFile.inputStream()) }
            jvmArgs(
                *localProperties.filter { it.key.toString().startsWith("flag") }
                    .map { "-D${it.key}=${it.value}" }
                    .toTypedArray(),
            )
        }
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "vLabeler"
            packageVersion = (version as String).split("-").first()
            copyright = "© 2022 sdercolin"
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            modules("java.sql", "jdk.charsets", "jdk.unsupported", "jdk.accessibility", "java.naming")

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
                bundleID = "com.sdercolin.vlabeler"

                if (project.localPropertiesFile.exists()) {
                    val properties = Properties().apply { load(project.localPropertiesFile.inputStream()) }
                    signing {
                        properties.getOrDefault("compose.desktop.mac.sign", "false").toString().toBoolean()
                            .let { sign.set(it) }
                        properties.getOrDefault("compose.desktop.mac.signing.identity", "").toString()
                            .let { identity.set(it) }
                    }
                    notarization {
                        properties.getOrDefault("compose.desktop.mac.notarization.appleID", "").toString()
                            .let { appleID.set(it) }
                        properties.getOrDefault("compose.desktop.mac.notarization.password", "").toString()
                            .let { password.set(it) }
                        properties.getOrDefault("compose.desktop.mac.notarization.teamID", "").toString()
                            .let { teamID.set(it) }
                    }
                }
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("0.45.2")
    enableExperimentalRules.set(true)
    disabledRules.set(setOf("no-wildcard-imports"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

task("createAppProperties") {
    dependsOn("jvmProcessResources")
    doLast {
        val appProperties = project.properties.filterKeys { it.startsWith("app.") }
        File("$buildDir/processedResources/jvm/main/app.properties").apply {
            createNewFile()
            writeText(appProperties.map { "${it.key}=${it.value}\n" }.joinToString(""))
        }
    }
}

tasks.findByName("jvmMainClasses")?.dependsOn("createAppProperties")

licenseReport {
    renderers = arrayOf(JsonReportRenderer())
}

task("checkLicenseReportUpdate") {
    dependsOn("generateLicenseReport")
    doLast {

        fun getDependencyArray(json: String): List<JsonElement> {
            return requireNotNull(Json.parseToJsonElement(json).jsonObject["dependencies"]).jsonArray
        }

        val excludedPatterns = listOf(
            "org.jetbrains.compose.desktop:desktop-jvm-.*",
            "org.jetbrains.skiko:skiko-awt-runtime-.*",
        )

        fun canIgnore(dependency: JsonObject): Boolean {
            val name = dependency["moduleName"]?.jsonPrimitive?.content ?: return false
            return excludedPatterns.any { name.matches(it.toRegex()) }
        }

        val generatedFile = File("$buildDir/reports/dependency-license/index.json")
        val sourceFile = File("src/jvmMain/resources/licenses.json")
        val generatedDependencies = getDependencyArray(generatedFile.readText())
        val sourceDependencies = getDependencyArray(sourceFile.readText())

        for (i in generatedDependencies.indices) {
            val generated = generatedDependencies[i].jsonObject
            val source = sourceDependencies[i].jsonObject
            if (canIgnore(generated)) continue
            if (generated.toString() != source.toString()) {
                throw IllegalStateException(
                    "License report is not up-to-date. Please run `./gradlew updateLicenseReport`",
                )
            }
        }
    }
}

tasks.findByName("test")?.dependsOn("checkLicenseReportUpdate")

task("updateLicenseReport") {
    dependsOn("generateLicenseReport")
    doLast {
        val generated = File("$buildDir/reports/dependency-license/index.json")
        val source = File("src/jvmMain/resources/licenses.json")
        generated.copyTo(source, overwrite = true)
    }
}
