import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.gradle.ext.Application
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
}

group = "me.crazylime.template"
version = "1.0.0"
description = "An example plugin for HyTale!"

// Root Path for Hytale
val hytaleServerRoot = "E:\\Hytale\\Server-EA\\2026.01.13-dcad8778f"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(files("$hytaleServerRoot\\Server\\HytaleServer.jar"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains:annotations:24.1.0")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

java {
    withSourcesJar()
    withJavadocJar()
}

// Ported: Quiet Javadoc warnings
tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

tasks.build {
    dependsOn("shadowJar")
}


// Reads manifest.json, updates version/pack flags, and writes it into the build output
tasks{
    processResources {
        val includesPack = true // Set to false if you don't have assets

        filesMatching("manifest.json") {
            // Read current file content
            val fileContent = file.readText()
            val json = JsonSlurper().parseText(fileContent) as MutableMap<String, Any>

            // Update fields
            json["Version"] = version
            json["IncludesAssetPack"] = includesPack

            // Return updated JSON string to Gradle to write to the JAR
            val updatedJson = JsonOutput.prettyPrint(JsonOutput.toJson(json))
            filter { updatedJson }
        }
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")

        // Relocate dependencies to avoid conflicts
        relocate("com.google.gson", "com.template.libs.gson")

        // Minimize JAR size (removes unused classes)
        minimize()
    }
}


// Ensure run directory exists
val serverRunDir = file("$projectDir/run")
if (!serverRunDir.exists()) {
    serverRunDir.mkdirs()
}

// Adds a "HytaleServer" run button to IntelliJ
idea {
    project {
        settings {
            runConfigurations {
                register<Application>("HytaleServer") {
                    mainClass = "com.hypixel.hytale.Main"
                    moduleName = "${project.name}.main"

                    val args = mutableListOf(
                        "--allow-op",
                        "--assets=$hytaleServerRoot\\Server\\Assets.zip"
                    )

                    val modPath = sourceSets.main.get().java.srcDirs.first().parentFile.absolutePath
                    args.add("--mods=$modPath")

                    programParameters = args.joinToString(" ")
                    workingDirectory = serverRunDir.absolutePath
                }
            }
        }
    }
}