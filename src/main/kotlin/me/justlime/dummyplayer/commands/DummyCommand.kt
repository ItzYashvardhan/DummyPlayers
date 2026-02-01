package me.justlime.dummyplayer.commands

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.AbstractCommand
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.permissions.HytalePermissions
import com.hypixel.hytale.server.core.universe.Universe
import me.justlime.dummyplayer.commands.subcommand.*
import me.justlime.dummyplayer.ui.UIManager
import java.util.concurrent.CompletableFuture

class DummyCommand : AbstractCommand("dummy", "Manage dummy players") {

    init {
        requirePermission(HytalePermissions.fromCommand("dummy"))
        addSubCommand(CreateDummyCommand())
        addSubCommand(DeleteDummyCommand())
        addSubCommand(UpdateDummyCommand())
        addSubCommand(FollowCommand())
        addSubCommand(CloneDummyCommand())
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