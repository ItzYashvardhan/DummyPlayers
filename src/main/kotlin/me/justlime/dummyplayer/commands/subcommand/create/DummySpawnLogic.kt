package me.justlime.dummyplayer.commands.subcommand.create

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.arguments.types.Coord
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeFloat
import com.hypixel.hytale.server.core.command.system.exceptions.GeneralCommandException
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.model.enums.DummyValidationResult
import me.justlime.dummyplayer.service.DummyPlayerService
import me.justlime.dummyplayer.utilities.Utilities
import kotlin.math.ceil
import kotlin.math.sqrt

object DummySpawnLogic {
    fun executeSpawn(
        playerRef: PlayerRef,
        world: World,
        refStore: Ref<EntityStore?>,
        name: String,
        amount: Int = 1,
        stack: Boolean = false,
        center: Boolean = false,
        gap: Float = 2.0f,
        xCoord: Coord? = null,
        yCoord: Coord? = null,
        zCoord: Coord? = null,
        yawRot: RelativeFloat? = null,
        pitchRot: RelativeFloat? = null,
        rollRot: RelativeFloat? = null
    ) {
        var safeAmount = amount
        if (safeAmount > 100) {
            playerRef.sendMessage(Message.raw("Limiting spawn to 100 dummies for performance."))
            safeAmount = 100
        }

        val executorTransform = world.entityStore.store.getComponent(refStore, TransformComponent.getComponentType())?.clone()
        val basePos = executorTransform?.position ?: Vector3d(0.0, 0.0, 0.0)
        val baseRot = executorTransform?.rotation ?: Vector3f(0.0f, 0.0f, 0.0f)

        val finalX = xCoord?.resolveXZ(basePos.x) ?: basePos.x
        val finalZ = zCoord?.resolveXZ(basePos.z) ?: basePos.z

        val finalY = try {
            yCoord?.resolveYAtWorldCoords(basePos.y, world, finalX, finalZ) ?: basePos.y
        } catch (e: GeneralCommandException) {
            playerRef.sendMessage(Message.raw(e.message ?: "Failed to resolve Y coordinate. Chunk not loaded."))
            return
        }

        val position = Vector3d(finalX, finalY, finalZ)

        val rotation = Vector3f(
            pitchRot?.resolve(baseRot.x) ?: baseRot.x,
            yawRot?.resolve(baseRot.y) ?: baseRot.y,
            rollRot?.resolve(baseRot.z) ?: baseRot.z
        )

        val (maxSuffix, _) = DummyPlayerService.getHighestDummySuffixAndName(name)

        val mySkinComponent = world.entityStore.store.getComponent(refStore, PlayerSkinComponent.getComponentType())
        val fallbackSkin = mySkinComponent?.playerSkin!!

        playerRef.sendMessage(Message.raw("Fetching skin for '$name'..."))

        Utilities.getSkin(name).thenAccept { foundSkin ->
            val finalSkin = foundSkin ?: fallbackSkin

            if (foundSkin == null) {
                playerRef.sendMessage(Message.raw("Could not find skin for '$name'. Using yours as fallback."))
            }

            val columns = ceil(sqrt(safeAmount.toDouble())).toInt()
            val rows = ceil(safeAmount.toDouble() / columns).toInt()

            val startX = if (center) position.x - ((columns - 1) * gap / 2.0) else position.x
            val startZ = if (center) position.z - ((rows - 1) * gap / 2.0) else position.z

            for (i in 0 until safeAmount) {
                val currentSuffix = maxSuffix + i + 1
                val dummyName = if (maxSuffix == 0 && safeAmount == 1) name else "${name}_$currentSuffix"

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
                                if (safeAmount == 1) {
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