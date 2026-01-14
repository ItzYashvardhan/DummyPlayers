package me.crazylime.template

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import me.crazylime.template.commands.CommandManager

class HytaleKotlinTemplate(init: JavaPluginInit) : JavaPlugin(init) {
    val hytaleLogger: HytaleLogger = HytaleLogger.forEnclosingClass()
    init {
        hytaleLogger.atInfo().log("Hello from " + this.name + " version " + this.manifest.version.toString());
    }

    override fun setup() {
        super.setup()
        CommandManager.registerCommands(name,this.manifest,commandRegistry)
    }

}
