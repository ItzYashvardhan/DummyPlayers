package me.justlime.dummyplayer.commands.subcommand.create

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.arguments.types.Coord
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeFloat
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class CreateDummyLocationVariant : AbstractPlayerCommand("Create dummy at location") {
    private val nameArgument: RequiredArg<String> = withRequiredArg("name", "Provide Player Name", ArgTypes.STRING)
    private val xArg: RequiredArg<Coord> = withRequiredArg("x", "X coordinate", ArgTypes.RELATIVE_DOUBLE_COORD)
    private val yArg: RequiredArg<Coord> = withRequiredArg("y", "Y coordinate", ArgTypes.RELATIVE_DOUBLE_COORD)
    private val zArg: RequiredArg<Coord> = withRequiredArg("z", "Z coordinate", ArgTypes.RELATIVE_DOUBLE_COORD)
    private val yawArg: OptionalArg<RelativeFloat> = withOptionalArg("yaw", "Yaw rotation", ArgTypes.RELATIVE_FLOAT)
    private val pitchArg: OptionalArg<RelativeFloat> = withOptionalArg("pitch", "Pitch rotation", ArgTypes.RELATIVE_FLOAT)
    private val rollArg: OptionalArg<RelativeFloat> = withOptionalArg("roll", "Roll rotation", ArgTypes.RELATIVE_FLOAT)

    override fun canGeneratePermission(): Boolean = false

    override fun execute(context: CommandContext, store: Store<EntityStore?>, refStore: Ref<EntityStore?>, playerRef: PlayerRef, world: World) {
        DummySpawnLogic.executeSpawn(
            playerRef = playerRef,
            world = world,
            refStore = refStore,
            name = nameArgument.get(context),
            xCoord = xArg.get(context),
            yCoord = yArg.get(context),
            zCoord = zArg.get(context),
            yawRot = yawArg.get(context),
            pitchRot = pitchArg.get(context),
            rollRot = rollArg.get(context)
        )
    }
}