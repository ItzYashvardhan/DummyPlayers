package me.justlime.dummyplayer.commands

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import me.justlime.dummyplayer.commands.subcommand.CreateDummyCommand
import me.justlime.dummyplayer.commands.subcommand.DeleteDummyCommand

class DummyCommand : AbstractCommandCollection("dummy", "Manage dummy players") {

    init {
        addSubCommand(_root_ide_package_.me.justlime.dummyplayer.commands.subcommand.CreateDummyCommand())
        addSubCommand(_root_ide_package_.me.justlime.dummyplayer.commands.subcommand.DeleteDummyCommand())
//        addSubCommand(CloneDummyCommand()) TODO
//        addSubCommand(FollowCommand())
//        addSubCommand(StopFollowCommand())
//        addSubCommand(SayCommand())
    }

    override fun canGeneratePermission(): Boolean = false
}





