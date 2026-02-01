package me.justlime.dummyplayer.commands.subcommand

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.permissions.HytalePermissions
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.service.DummyPlayerService
import me.justlime.dummyplayer.utilities.Utilities

class UpdateDummyCommand : AbstractPlayerCommand("update", "Update a dummy's skin") {

    private val dummyNameArg: RequiredArg<String> = withRequiredArg("name", "Name of the dummy", ArgTypes.STRING)
    private val skinSourceArg: OptionalArg<String> = withOptionalArg("skin", "Username to copy skin from", ArgTypes.STRING)

    override fun canGeneratePermission(): Boolean = false

    init {
        requirePermission(HytalePermissions.fromCommand("dummy.update"))
    }

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore?>,
        refStore: Ref<EntityStore?>,
        playerRef: PlayerRef,
        world: World
    ) {
        val dummyName = dummyNameArg.get(context)
        val skinSource = skinSourceArg.get(context) ?: dummyName

        val dummyRef = DummyPlayerService.getDummy(dummyName)
        if (dummyRef == null) {
            context.sendMessage(Message.raw("A dummy named '$dummyName' does not exist!"))
            return
        }

        context.sendMessage(Message.raw("Fetching skin from player '$skinSource' for dummy '$dummyName'..."))
        Utilities.refreshSkin(skinSource).thenAccept { newSkin ->
            if (newSkin == null) {
                playerRef.sendMessage(Message.raw("Could not find skin for user '$skinSource'."))
                return@thenAccept
            }
            world.execute {
                if (DummyPlayerService.updateSkin(dummyRef, newSkin)) {
                    playerRef.sendMessage(Message.raw("Updated '$dummyName' with $skinSource's skin."))
                } else {
                    playerRef.sendMessage(Message.raw("Failed to update skin for: $dummyName"))
                }
            }
        }
    }
}