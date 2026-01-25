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
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.impl.DummyPlayerFactory

class CreateDummyCommand : AbstractPlayerCommand("create", "Create a dummy player") {
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
        val transform = world.entityStore.store.getComponent(refStore, TransformComponent.getComponentType())
        val position = transform?.position ?: Vector3d(0.0, 0.0, 0.0)

        // Get the skin of the player executing the command
        val skinComponent = world.entityStore.store.getComponent(refStore, PlayerSkinComponent.getComponentType())
        val skin = skinComponent?.playerSkin

        _root_ide_package_.me.justlime.dummyplayer.impl.DummyPlayerFactory.spawnDummy(world, name, position, skin)
        context.sendMessage(Message.raw("Created dummy player: $name"))
    }
}
