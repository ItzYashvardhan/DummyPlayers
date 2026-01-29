package me.justlime.dummyplayer.commands.subcommand

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.permissions.HytalePermissions
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.enums.DummyValidationResult
import me.justlime.dummyplayer.service.DummyPlayerFactory
import me.justlime.dummyplayer.utilities.Utilities

class CreateDummyCommand : AbstractPlayerCommand("create", "Create a dummy player") {
    var nameArgument: RequiredArg<String> = withRequiredArg("name", "Provide Player Name", ArgTypes.STRING)
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



        // Get position from the executing player
        val transform = world.entityStore.store.getComponent(refStore, TransformComponent.getComponentType())
        val position = transform?.position ?: Vector3d(0.0, 0.0, 0.0)

        // Prepare the Fallback Skin
        val mySkinComponent = world.entityStore.store.getComponent(refStore, PlayerSkinComponent.getComponentType())
        val fallbackSkin = mySkinComponent?.playerSkin

        context.sendMessage(Message.raw("Fetching skin for '$name'..."))

        Utilities.getSkin(name).thenAccept { foundSkin ->
            val finalSkin = foundSkin ?: fallbackSkin

            if (foundSkin == null) {
                playerRef.sendMessage(Message.raw("Could not find skin for '$name'. Using yours as fallback."))
            }

            world.execute {
                DummyPlayerFactory.spawnDummy(world, name, position, finalSkin).thenAccept { result ->
                    when (result) {
                        is DummyValidationResult.Success -> {
                            playerRef.sendMessage(Message.raw("Created dummy: $name"))
                        }
                        is DummyValidationResult.AlreadyExists -> {
                            playerRef.sendMessage(Message.raw("A dummy named '$name' already exists!"))
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