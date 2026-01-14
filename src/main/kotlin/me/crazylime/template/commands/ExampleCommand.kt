package me.crazylime.template.commands

import com.hypixel.hytale.protocol.GameMode
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase

class ExampleCommand(pluginName: String, description: String) : CommandBase(pluginName, description) {

    init {
        println("testPrints a test message from the $pluginName plugin.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
    }

    protected override fun executeSync(context: CommandContext) {
        context.sendMessage(Message.raw("Hello from the $name $description plugin!"));
    }

}