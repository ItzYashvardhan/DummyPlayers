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

class StopFollowCommand : AbstractPlayerCommand("stopfollow", "Make a dummy stop following") {
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
        _root_ide_package_.me.justlime.dummyplayer.behaviour.DummyAI.clearBehaviors(dummyName)
        context.sendMessage(Message.raw("$dummyName stopped following."))
    }
}
