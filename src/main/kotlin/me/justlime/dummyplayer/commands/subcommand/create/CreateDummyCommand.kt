package me.justlime.dummyplayer.commands.subcommand.create

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.permissions.HytalePermissions

class CreateDummyCommand : AbstractCommandCollection( "create","Create a dummy player") {
    init {
        requirePermission(HytalePermissions.fromCommand("dummy.create"))
        addUsageVariant(CreateDummySimpleVariant())
        addUsageVariant(CreateDummyBulkVariant())
        addUsageVariant(CreateDummyLocationVariant())
        addUsageVariant(CreateDummyBulkLocationVariant())
    }
}

