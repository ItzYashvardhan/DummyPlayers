package me.justlime.dummyplayer.commands.subcommand

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.permissions.HytalePermissions
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.service.DummyPlayerService

class ChatDummyCommand : AbstractPlayerCommand("chat", "Delete a dummy player") {

    var nameArgument: RequiredArg<String> = withRequiredArg("name", "Provide Player Name", ArgTypes.STRING)
    var chatArgument: RequiredArg<String> = withRequiredArg("message", "Message to send", ArgTypes.STRING)

    init {
        requirePermission(HytalePermissions.fromCommand("dummy.chat"))
    }

    override fun canGeneratePermission(): Boolean = false

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore?>,
        refStore: Ref<EntityStore?>,
        playerRef: PlayerRef,
        world: World
    ) {
        val name = nameArgument.get(context)
        val dummyRef = DummyPlayerService.getDummy(name)
        if (dummyRef == null) {
            context.sendMessage(Message.raw("Could not find dummy player: $name"))
            return
        }
        val message = chatArgument.get(context)
        if (message == null) {
            context.sendMessage(Message.raw("Please provide a message to send."))
            return
        }

        if (message.startsWith("/")) {
            HytaleServer.get().commandManager.handleCommand(dummyRef, message.removePrefix("/"))
        } else {
            dummyRef.packetHandler.handle(ChatMessage(message))
        }

    }
}