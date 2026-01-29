package me.justlime.dummyplayer.ecs

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import java.util.*

class DummyComponent(
    var controllerUuid: UUID? = null, // Default null for "Empty/Registry" instance
    var isActive: Boolean = true
) : Component<EntityStore> {

    override fun clone(): Component<EntityStore> {
        return DummyComponent(controllerUuid, isActive)
    }
}