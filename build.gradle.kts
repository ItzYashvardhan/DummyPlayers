import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.gradle.ext.Application
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "8.3.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
}

group = "me.crazylime"
version = "0.0.1"

// Root Path for Hytale
val hytaleServerRoot = "E:\\Hytale\\Server-EA\\2026.01.13-dcad8778f"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(files("$hytaleServerRoot\\Server\\HytaleServer.jar"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
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
tasks.processResources {
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