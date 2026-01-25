package me.justlime.dummyplayer.behaviour

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

interface DummyBehavior {
    fun tick(world: World, dummyRef: Ref<EntityStore>, dummyName: String)
}