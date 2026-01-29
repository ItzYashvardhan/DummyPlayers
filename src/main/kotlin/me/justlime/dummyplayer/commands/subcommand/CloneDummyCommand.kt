package me.justlime.dummyplayer.commands.subcommand

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class CloneDummyCommand : AbstractPlayerCommand("clone", "Clone a dummy player") {

    val targetArgs: OptionalArg<String> = withOptionalArg("name", "Provide Player Name", ArgTypes.STRING)
    override fun canGeneratePermission(): Boolean = false

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore?>,
        refStore: Ref<EntityStore?>,
        playerRef: PlayerRef,
        world: World
    ) {
        val targetName = if (context.provided(targetArgs)) {
            targetArgs.get(context)
        } else {
            playerRef.username
        }

        // Find the target player/dummy
        var targetRef: Ref<EntityStore>? = null

        // Check real players first
        val playerRefs = world.playerRefs
        for (pRef in playerRefs) {
            if (pRef.username.equals(targetName, ignoreCase = true)) {
                targetRef = pRef.reference
                break
            }
        }

        // If not found, check our tracked dummies
        if (targetRef == null) {
//            targetRef = _root_ide_package_.me.justlime.dummyplayer.service.DummyPlayerFactory.getDummy(targetName)
        }

        if (targetRef != null) {
            // Get the skin of the player executing the command (the cloner)
            val skinComponent = world.entityStore.store.getComponent(refStore, PlayerSkinComponent.getComponentType())
            val requesterSkin = skinComponent?.playerSkin

//            val DummyPlayerFactory.cloneDummy(world, targetRef, requesterSkin)
            context.sendMessage(Message.raw("Cloned $targetName"))
        } else {
            context.sendMessage(Message.raw("Could not find player or dummy: $targetName"))
        }
    }
}
