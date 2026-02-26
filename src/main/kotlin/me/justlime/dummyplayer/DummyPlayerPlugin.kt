package me.justlime.dummyplayer

import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.commands.CommandManager
import me.justlime.dummyplayer.component.DummyPlayerComponent
import me.justlime.dummyplayer.listener.ListenerManager

class DummyPlayerPlugin(init: JavaPluginInit) : JavaPlugin(init) {

    companion object {
        private lateinit var instance: DummyPlayerPlugin

        fun get(): DummyPlayerPlugin {
            return instance
        }
    }

    lateinit var dummyPlayerComponentType: ComponentType<EntityStore, DummyPlayerComponent>
        private set

    init {
        instance = this
        logger.atInfo().log("Hello from " + this.name + " version " + this.manifest.version.toString())
    }

    override fun setup() {
        super.setup()
        registerComponent()
        CommandManager.registerCommands(this)
        val listener = ListenerManager(this)
        listener.register()
//        listener.eventTest()
//        entityStoreRegistry.registerSystem(DummyDeathSystem()) TODO
    }

    private fun registerComponent() {
        dummyPlayerComponentType = this.entityStoreRegistry.registerComponent(
            DummyPlayerComponent::class.java,
            ::DummyPlayerComponent
        )
    }
}
