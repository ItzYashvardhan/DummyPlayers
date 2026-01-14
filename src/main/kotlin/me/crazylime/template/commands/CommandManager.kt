package me.crazylime.template.commands

import com.hypixel.hytale.common.plugin.PluginManifest
import com.hypixel.hytale.server.core.command.system.CommandRegistry

object CommandManager {

    fun registerCommands(name: String, manifest: PluginManifest, registry: CommandRegistry) {

        val command = ExampleCommand(name, manifest.description.toString())
        registry.registerCommand(command)
    }
}