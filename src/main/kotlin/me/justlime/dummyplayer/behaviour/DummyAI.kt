package me.justlime.dummyplayer.behaviour

import com.hypixel.hytale.server.core.universe.world.World
import java.util.concurrent.ConcurrentHashMap


object DummyAI {

    private val behaviors = ConcurrentHashMap<String, MutableList<me.justlime.dummyplayer.behaviour.DummyBehavior>>()

    fun addBehavior(dummyName: String, behavior: me.justlime.dummyplayer.behaviour.DummyBehavior) {
        behaviors.computeIfAbsent(dummyName) { mutableListOf() }.add(behavior)
    }

    fun clearBehaviors(dummyName: String) {
        behaviors.remove(dummyName)
    }

    fun tick(world: World) {
        behaviors.forEach { (dummyName, behaviorList) ->
            val dummyRef = _root_ide_package_.me.justlime.dummyplayer.service.DummyPlayerFactory.getDummy(dummyName)
            if (dummyRef == null || !dummyRef.isValid) {
                behaviors.remove(dummyName)
                return@forEach
            }

            behaviorList.forEach { behavior ->
//                behavior.tick(world, dummyRef, dummyName)
            }
        }
    }
}

