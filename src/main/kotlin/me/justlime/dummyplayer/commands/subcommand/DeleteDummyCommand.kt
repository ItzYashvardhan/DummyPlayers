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
import me.justlime.dummyplayer.impl.DummyPlayerFactory

class DeleteDummyCommand : AbstractPlayerCommand("delete", "Delete a dummy player") {
    var nameArgument: RequiredArg<String> = withRequiredArg("name", "Provide Player Name", ArgTypes.STRING)
    override fun canGeneratePermission(): Boolean = false
    override fun execute(
        context: CommandContext,
        store: Store<EntityStore?>,
        refStore: Ref<EntityStore?>,
        playerRef: PlayerRef,
        world: World
    ) {
        val name = nameArgument.get(context)
        if (_root_ide_package_.me.justlime.dummyplayer.impl.DummyPlayerFactory.deleteDummy(world, name)) {
            context.sendMessage(Message.raw("Deleted dummy player: $name"))
        } else {
            context.sendMessage(Message.raw("Could not find dummy player: $name"))
        }
    }
}
