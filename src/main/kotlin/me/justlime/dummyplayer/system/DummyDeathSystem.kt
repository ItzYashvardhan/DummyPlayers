package me.justlime.dummyplayer.system

import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems
import com.hypixel.hytale.server.core.modules.entity.damage.DeferredCorpseRemoval
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.component.DummyPlayerComponent

class DummyDeathSystem : DeathSystems.OnDeathSystem() {
    private val deferredCorpseRemovalComponentType: ComponentType<EntityStore?, DeferredCorpseRemoval?> =
        DeferredCorpseRemoval.getComponentType()

    private val query: Query<EntityStore> = Query.and(
        DummyPlayerComponent.getComponentType(),
        PlayerRef.getComponentType()
    )

    override fun getQuery(): Query<EntityStore> {
        return query
    }

    override fun onComponentAdded(
        ref: Ref<EntityStore>,
        component: DeathComponent,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        val isDead = store.getArchetype(ref).contains(DeathComponent.getComponentType())
        commandBuffer.addComponent<DeferredCorpseRemoval?>(
            ref,
            this.deferredCorpseRemovalComponentType,
            DeferredCorpseRemoval(1.0)
        )

        if (isDead) {
            val world = store.externalData.world
            world.execute {
                if (ref.isValid) {
                    val isDead = store.getArchetype(ref).contains(DeathComponent.getComponentType())

                    if (isDead) {
                        DeathComponent.respawn(store, ref)
                    }
                }
            }
        }
    }
}