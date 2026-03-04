package me.justlime.dummyplayer.commands.subcommand.create

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.permissions.HytalePermissions
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class CreateDummyBulkVariant : AbstractPlayerCommand( "Create multiple dummies") {
    private val nameArgument: RequiredArg<String> = withRequiredArg("name", "Provide Player Name", ArgTypes.STRING)
    private val amountArgument: RequiredArg<Int> = withRequiredArg("amount", "Amount of dummies to create", ArgTypes.INTEGER)
    private val stackArgument: OptionalArg<Boolean> = withOptionalArg("stack", "Spawn all on single position", ArgTypes.BOOLEAN)
    private val centerArgument: OptionalArg<Boolean> = withOptionalArg("center", "Center formation around player", ArgTypes.BOOLEAN)
    private val gapArgument: OptionalArg<Float> = withOptionalArg("gap", "Gap between dummies", ArgTypes.FLOAT)

    init {
        requirePermission(HytalePermissions.fromCommand("dummy.create"))
    }
    
    override fun execute(context: CommandContext, store: Store<EntityStore?>, refStore: Ref<EntityStore?>, playerRef: PlayerRef, world: World) {
        DummySpawnLogic.executeSpawn(
            playerRef = playerRef,
            world = world,
            refStore = refStore,
            name = nameArgument.get(context),
            amount = amountArgument.get(context),
            stack = stackArgument.get(context) ?: false,
            center = centerArgument.get(context) ?: false,
            gap = gapArgument.get(context) ?: 2.0f
        )
    }
}