package me.justlime.dummyplayer.utilities

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import me.justlime.dummyplayer.DummyPlayerPlugin
import java.io.File

object ResourceLoader {
    lateinit var menuContentHtml: String
    fun load(plugin: DummyPlayerPlugin){
        menuContentHtml = loadAndSaveResource(plugin, "html/Menu.html", "Menu.html")
    }


    private fun loadAndSaveResource(plugin: JavaPlugin, resourcePath: String, outputFileName: String, replace: Boolean = false): String {
        // 1. Get the stream
        val inputStream = plugin.javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Resource not found in JAR: $resourcePath")

        // 2. Read content (Kotlin extension method handles closing)
        val defaultContent = String(inputStream.readBytes(), Charsets.UTF_8)

        // 3. Setup File
        val dataFolder = File(plugin.dataDirectory.toString(), "html")
        if (!dataFolder.exists()) dataFolder.mkdirs()

        val destinationFile = File(dataFolder, outputFileName)

        // 4. Write Logic (Preserve user edits unless 'replace' is true)
        if (replace || !destinationFile.exists()) {
            destinationFile.writeText(defaultContent)
            return defaultContent
        } else {
            // Return the existing file content (what the user has on disk)
            return destinationFile.readText()
        }
    }
}