package me.justlime.dummyplayer.commands

import com.hypixel.hytale.protocol.GameMode
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.AbstractCommand
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.universe.Universe
import me.justlime.dummyplayer.commands.subcommand.CreateDummyCommand
import me.justlime.dummyplayer.commands.subcommand.DeleteDummyCommand
import me.justlime.dummyplayer.ui.UIManager
import java.util.concurrent.CompletableFuture

class DummyCommand : AbstractCommand("dummy", "Manage dummy players") {

    init {
        this.setPermissionGroup(GameMode.Creative)

        // Add your subcommands here
        addSubCommand(CreateDummyCommand())
        addSubCommand(DeleteDummyCommand())
    }

    override fun execute(context: CommandContext): CompletableFuture<Void?>? {
        if (!context.isPlayer) return null
        val playerRef = Universe.get().getPlayer(context.sender().uuid) ?: return null
        val worldUuid = playerRef.worldUuid
        if (worldUuid == null) {
            context.sendMessage(Message.raw("Player not in a world!"))
            return null
        }
        val world = Universe.get().getWorld(worldUuid)
        if (world == null) {
            context.sendMessage(Message.raw("World not found!"))
            return null
        }
        world.execute {
            UIManager.open(playerRef)
        }
        return null
    }

    override fun canGeneratePermission(): Boolean = true
}