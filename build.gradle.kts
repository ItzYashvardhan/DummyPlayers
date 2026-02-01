import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.gradle.ext.Application
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.idea.ext)
}

// Read manifest.json for project details
@Suppress("UNCHECKED_CAST")
val manifest = JsonSlurper().parseText(file("src/main/resources/manifest.json").readText()) as Map<String, Any>
group = manifest["Group"]!!
version = manifest["Version"]!!
description = manifest["Description"] as? String

// Root Path for Hytale
val hytaleServerRoot = "E:\\Hytale\\Server-EA\\2026.01.24-6e2d4fc36"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    compileOnly(files("libs/HytaleServer.jar"))
    implementation(files("libs/HyUI-0.5.10-all.jar"))
    implementation(libs.annotations)
    compileOnly(libs.gson)
}

// Allow compileOnly dependencies to be available at runtime (for the server run config)
configurations.runtimeClasspath.get().extendsFrom(configurations.compileOnly.get())

val targetJavaVersion = 25
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
tasks {
    processResources {
        val includesPack = true

        filesMatching("manifest.json") {
            // Read current file content
            val fileContent = file.readText()
            @Suppress("UNCHECKED_CAST")
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
        relocate("au.ellie.hyui", "me.justlime.dummyplayer.libs.hyui")

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
                        "--assets=$hytaleServerRoot\\Assets.zip"
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