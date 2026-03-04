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

@Suppress("UNCHECKED_CAST")
val manifest = JsonSlurper().parseText(file("src/main/resources/manifest.json").readText()) as Map<String, Any>
group = manifest["Group"]!!
version = manifest["Version"]!!
description = manifest["Description"] as? String

val user: String = System.getProperty("user.name") ?: ""
val hytaleServerRoot = "C:/Users/$user/AppData/Roaming/Hytale/install/release/package/game/latest"

val shadowBundle: Configuration? by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(configurations.implementation.get())
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    compileOnly(files("$hytaleServerRoot/Server/HytaleServer.jar"))
    implementation(files("libs/HyUI-0.9.0.jar"))
    implementation(libs.annotations)
    compileOnly(libs.gson)
    implementation("org.bstats:bstats-hytale:3.2.1")
}

configurations.runtimeClasspath.get().extendsFrom(configurations.compileOnly.get())

val targetJavaVersion = 25
kotlin {
    jvmToolchain(targetJavaVersion)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

tasks.build {
    dependsOn("shadowJar")
}

val serverRunDir = file("$projectDir/run")
if (!serverRunDir.exists()) {
    serverRunDir.mkdirs()
}

tasks {
    processResources {
        val includesPack = true

        filesMatching("manifest.json") {
            val fileContent = file.readText()
            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parseText(fileContent) as MutableMap<String, Any>

            json["Version"] = version
            json["IncludesAssetPack"] = includesPack

            val updatedJson = JsonOutput.prettyPrint(JsonOutput.toJson(json))
            filter { updatedJson }
        }
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        configurations = listOf(shadowBundle)
        dependencies {
            exclude { it.moduleGroup != "org.bstats" }
        }
        relocate("au.ellie.hyui", "me.justlime.dummyplayer.libs.hyui")
        relocate("org.bstats", "me.justlime.dummyplayer.libs.bstats")
        minimize()
    }
}

idea {
    project {
        settings {
            runConfigurations {
                register<Application>("HytaleServer") {
                    mainClass = "com.hypixel.hytale.Main"
                    moduleName = "${project.name}.main"
                    jvmArgs = "-Dbstats.relocatecheck=false"
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