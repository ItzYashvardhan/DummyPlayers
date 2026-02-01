package me.justlime.dummyplayer.component

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.DummyPlayerPlugin
import java.util.*

class DummyPlayerComponent : Component<EntityStore> {
    var controllerUuid: UUID? = null
    var isActive: Boolean = true
    companion object {
        fun getComponentType(): ComponentType<EntityStore, DummyPlayerComponent> {
            return DummyPlayerPlugin.get().dummyPlayerComponentType
        }
    }

    override fun clone(): Component<EntityStore> {
        val copy = DummyPlayerComponent()
        copy.controllerUuid = controllerUuid
        copy.isActive = isActive
        return copy
    }
}
