package me.justlime.dummyplayer.ecs

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.pov.DummyInputQueue

class DummySystem(private val dummyType: ComponentType<EntityStore, DummyComponent>) : EntityTickingSystem<EntityStore>() {

    override fun getQuery(): Query<EntityStore> {
        return Query.and(PlayerRef.getComponentType(), dummyType)
    }

    override fun tick(
        dt: Float,
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        buffer: CommandBuffer<EntityStore>
    ) {
        return //TODO
        val dummyComp = chunk.getComponent(index, dummyType) ?: return
        val controllerUuid = dummyComp.controllerUuid ?: return
        val input = DummyInputQueue.queue.find { it.dummyUuid == controllerUuid }

        if (input != null) {
            DummyInputQueue.queue.remove(input)
        }
    }
}