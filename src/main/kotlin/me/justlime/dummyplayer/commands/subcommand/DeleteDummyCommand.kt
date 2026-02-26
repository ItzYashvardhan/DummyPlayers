package me.justlime.dummyplayer.commands.subcommand

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.permissions.HytalePermissions
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.service.DummyPlayerService

class DeleteDummyCommand : AbstractPlayerCommand("delete", "Delete a dummy player") {

    var nameArgument: RequiredArg<String> = withRequiredArg("name", "Provide Player/Base Name", ArgTypes.STRING)
    var bulkArgument: OptionalArg<Boolean> =
        withOptionalArg("bulk", "Delete all dummies matching base name", ArgTypes.BOOLEAN)

    init {
        requirePermission(HytalePermissions.fromCommand("dummy.delete"))
    }

    override fun canGeneratePermission(): Boolean = false

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore?>,
        refStore: Ref<EntityStore?>,
        playerRef: PlayerRef,
        world: World
    ) {
        val name = nameArgument.get(context)
        val bulk = bulkArgument.get(context) ?: false

        if (name == "*") {
            val allNames = DummyPlayerService.getDummyNames()
            if (allNames.isEmpty()) {
                context.sendMessage(Message.raw("No dummies found to delete."))
                return
            }

            if (!bulk){
                context.sendMessage(Message.raw("To delete all dummies, you must set the bulk argument to true: /dummy delete * --bulk=true"))
                return
            }

            var count = 0
            allNames.forEach { dummyName ->
                if (DummyPlayerService.deleteDummy(dummyName)) {
                    count++
                }
            }
            context.sendMessage(Message.raw("Deleted all dummies ($count total)."))
            return
        }

        if (name == null) {
            context.sendMessage(Message.raw("Please provide a dummy name or use the all flag."))
            return
        }

        if (bulk) {
            val allNames = DummyPlayerService.getDummyNames()
            val toDelete = allNames.filter { it == name || it.startsWith("${name}_") }

            if (toDelete.isEmpty()) {
                context.sendMessage(Message.raw("Could not find any bulk dummies with base name: $name"))
                return
            }

            var count = 0
            toDelete.forEach { dummyName ->
                if (DummyPlayerService.deleteDummy(dummyName)) {
                    count++
                }
            }
            context.sendMessage(Message.raw("Deleted $count bulk dummies for base name: $name"))
            return
        }

        if (DummyPlayerService.deleteDummy(name)) {
            context.sendMessage(Message.raw("Deleted dummy player: $name"))
        } else {
            context.sendMessage(Message.raw("Could not find dummy player: $name"))
        }
    }
}