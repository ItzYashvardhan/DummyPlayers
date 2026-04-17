package me.justlime.dummyplayer.commands.subcommand.create

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.permissions.HytalePermissions
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class CreateDummySimpleVariant : AbstractPlayerCommand("Create a single dummy") {
    init {
        requirePermission(HytalePermissions.fromCommand("dummy.create"))
    }

    private val nameArgument = withRequiredArg("name", "Provide Player Name", ArgTypes.STRING)
    private val pingArg = withOptionalArg("ping", "Simulated ping in ms", ArgTypes.INTEGER)

    override fun execute(context: CommandContext, store: Store<EntityStore?>, refStore: Ref<EntityStore?>, playerRef: PlayerRef, world: World) {
        DummySpawnLogic.executeSpawn(
            playerRef = playerRef,
            world = world,
            refStore = refStore,
            name = nameArgument.get(context),
            ping = (pingArg.get(context) ?: 0).toLong()
        )
    }
}




