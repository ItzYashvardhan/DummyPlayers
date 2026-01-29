package me.justlime.dummyplayer

import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.commands.CommandManager
import me.justlime.dummyplayer.ecs.DummyComponent
import me.justlime.dummyplayer.ecs.DummySystem
import me.justlime.dummyplayer.listener.ListenerManager

class DummyPlayerPlugin(init: JavaPluginInit) : JavaPlugin(init) {
    companion object {
        lateinit var DUMMY_COMPONENT_TYPE: ComponentType<EntityStore, DummyComponent>
    }


    init {
        logger.atInfo().log("Hello from " + this.name + " version " + this.manifest.version.toString());
    }

    override fun setup() {
        super.setup()
        CommandManager.registerCommands(this)
        ListenerManager(this).register()
        DUMMY_COMPONENT_TYPE = this.entityStoreRegistry.registerComponent(DummyComponent::class.java, ::DummyComponent)
        this.entityStoreRegistry.registerSystem(DummySystem(DUMMY_COMPONENT_TYPE))
    }
}
