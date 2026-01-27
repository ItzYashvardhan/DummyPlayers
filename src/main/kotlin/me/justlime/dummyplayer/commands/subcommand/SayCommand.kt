package me.justlime.dummyplayer.commands.subcommand

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class SayCommand : AbstractPlayerCommand("say", "Make a dummy say something") {
    var dummyNameArg: RequiredArg<String> = withRequiredArg("dummy", "Dummy Name", ArgTypes.STRING)
    var messageArg: RequiredArg<String> = withRequiredArg("message", "Message", ArgTypes.STRING)
    
    override fun canGeneratePermission(): Boolean = false

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore?>,
        refStore: Ref<EntityStore?>,
        playerRef: PlayerRef,
        world: World
    ) {
        val dummyName = dummyNameArg.get(context)
        val message = messageArg.get(context)
        
        if (_root_ide_package_.me.justlime.dummyplayer.service.DummyPlayerFactory.getDummy(dummyName) != null) {
            // One-time action, doesn't need a persistent behavior
             world.playerRefs.forEach { p ->
                 p.sendMessage(Message.raw("<$dummyName> $message"))
             }
        } else {
            context.sendMessage(Message.raw("Dummy not found: $dummyName"))
        }
    }
}
