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
import me.justlime.dummyplayer.behaviour.DummyAI

class FollowCommand : AbstractPlayerCommand("follow", "Make a dummy follow you") {
    var dummyNameArg: RequiredArg<String> = withRequiredArg("dummy", "Dummy Name", ArgTypes.STRING)
    
    override fun canGeneratePermission(): Boolean = false

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore?>,
        refStore: Ref<EntityStore?>,
        playerRef: PlayerRef,
        world: World
    ) {
        val dummyName = dummyNameArg.get(context)
        val targetName = playerRef.username
        
        if (me.justlime.dummyplayer.service.DummyPlayerFactory.getDummy(dummyName) != null) {
            // Clear previous behaviors to avoid conflicts (optional)
           DummyAI.clearBehaviors(dummyName)
           DummyAI.addBehavior(dummyName,
                _root_ide_package_.me.justlime.dummyplayer.ai.FollowBehavior(targetName)
            )
            context.sendMessage(Message.raw("$dummyName is now following you."))
        } else {
            context.sendMessage(Message.raw("Dummy not found: $dummyName"))
        }
    }
}
