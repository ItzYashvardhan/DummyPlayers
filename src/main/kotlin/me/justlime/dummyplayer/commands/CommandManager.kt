package me.justlime.dummyplayer.commands

import me.justlime.dummyplayer.DummyPlayerPlugin

object CommandManager {

    fun registerCommands(plugin: DummyPlayerPlugin) {
        val registry = plugin.commandRegistry
        val dummyCommand = DummyCommand()
        registry.registerCommand(dummyCommand)
    }
}