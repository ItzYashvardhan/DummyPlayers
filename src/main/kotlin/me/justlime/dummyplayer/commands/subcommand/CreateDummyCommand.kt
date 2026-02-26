package me.justlime.dummyplayer.commands.subcommand

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.permissions.HytalePermissions
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.model.enums.DummyValidationResult
import me.justlime.dummyplayer.service.DummyPlayerService
import me.justlime.dummyplayer.utilities.Utilities
import kotlin.math.ceil
import kotlin.math.sqrt

class CreateDummyCommand : AbstractPlayerCommand("create", "Create a dummy player") {
    var nameArgument: RequiredArg<String> = withRequiredArg("name", "Provide Player Name", ArgTypes.STRING)
    var amountArgument: OptionalArg<Int> = withOptionalArg("amount", "Amount of dummies to create", ArgTypes.INTEGER)
    var stackArgument: OptionalArg<Boolean> = withOptionalArg("stack", "Spawn all on single position", ArgTypes.BOOLEAN)
    var centerArgument: OptionalArg<Boolean> = withOptionalArg("center", "Center formation around player", ArgTypes.BOOLEAN)
    var gapArgument: OptionalArg<Float> = withOptionalArg("gap", "Gap between dummies", ArgTypes.FLOAT)

    override fun canGeneratePermission(): Boolean = false

    init {
        requirePermission(HytalePermissions.fromCommand("dummy.create"))
    }

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore?>,
        refStore: Ref<EntityStore?>,
        playerRef: PlayerRef,
        world: World
    ) {
        val name = nameArgument.get(context)
        var amount = amountArgument.get(context) ?: 1
        val stack = stackArgument.get(context) ?: false
        val center = centerArgument.get(context) ?: false
        val gap = gapArgument.get(context)?.toDouble() ?: 2.0

        if (amount > 100) {
            playerRef.sendMessage(Message.raw("Limiting spawn to 100 dummies for performance."))
            amount = 100
        }

        val transform = world.entityStore.store.getComponent(refStore, TransformComponent.getComponentType())?.clone()
        var position = transform?.position ?: Vector3d(0.0, 0.0, 0.0)
        val rotation = transform?.rotation ?: Vector3f(0.0f, 0.0f, 0.0f)

        val (maxSuffix, lastDummyName) = DummyPlayerService.getHighestDummySuffixAndName(name)

        if (maxSuffix > 0 || DummyPlayerService.getDummyNames().contains(name)) {
            val lastDummyRef = DummyPlayerService.getDummy(lastDummyName)
            val lastTransform = lastDummyRef?.reference?.let {
                world.entityStore.store.getComponent(it, TransformComponent.getComponentType())
            }
            if (lastTransform != null) {
                position = lastTransform.position
            }
        }

        val mySkinComponent = world.entityStore.store.getComponent(refStore, PlayerSkinComponent.getComponentType())
        val fallbackSkin = mySkinComponent?.playerSkin!!

        context.sendMessage(Message.raw("Fetching skin for '$name'..."))

        Utilities.getSkin(name).thenAccept { foundSkin ->
            val finalSkin = foundSkin ?: fallbackSkin

            if (foundSkin == null) {
                playerRef.sendMessage(Message.raw("Could not find skin for '$name'. Using yours as fallback."))
            }

            val columns = ceil(sqrt(amount.toDouble())).toInt()
            val rows = ceil(amount.toDouble() / columns).toInt()

            val startX = if (center) position.x - ((columns - 1) * gap / 2.0) else position.x
            val startZ = if (center) position.z - ((rows - 1) * gap / 2.0) else position.z

            for (i in 0 until amount) {
                val currentSuffix = maxSuffix + i + 1
                val dummyName = if (maxSuffix == 0 && amount == 1) name else "${name}_$currentSuffix"

                val spawnPos = if (stack) {
                    position
                } else {
                    val row = i / columns
                    val col = i % columns
                    Vector3d(startX + (col * gap), position.y, startZ + (row * gap))
                }

                world.execute {
                    DummyPlayerService.spawnDummy(world, dummyName, spawnPos, rotation, finalSkin).thenAccept { result ->
                        when (result) {
                            is DummyValidationResult.Success -> {
                                playerRef.sendMessage(Message.raw("Created dummy: $dummyName"))
                            }
                            is DummyValidationResult.AlreadyExists -> {
                                if (amount == 1) {
                                    playerRef.sendMessage(Message.raw("A dummy named '$dummyName' already exists!"))
                                }
                            }
                            is DummyValidationResult.ConnectionDenied -> {
                                playerRef.sendMessage(Message.raw("Spawn blocked: ${result.reason}"))
                            }
                            is DummyValidationResult.Failure -> {
                                playerRef.sendMessage(Message.raw("Error creating dummy: ${result.message}"))
                                result.exception?.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}