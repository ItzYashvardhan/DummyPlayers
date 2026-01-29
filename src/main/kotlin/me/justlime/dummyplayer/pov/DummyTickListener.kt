package me.justlime.dummyplayer.pov

import com.hypixel.hytale.event.EventRegistry
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput
import me.justlime.dummyplayer.service.DummyPlayerFactory

class DummyTickListener(val eventRegistry: EventRegistry) {

    fun register() {
    }

    private fun processInputs() {
        while (!DummyInputQueue.queue.isEmpty()) {
            val input = DummyInputQueue.queue.poll() ?: continue

            val dummyRef = DummyPlayerFactory.getDummy(input.dummyUuid) ?: continue
            val dummyEntity = dummyRef.reference ?: continue 
            val store = dummyEntity.store

            // 1. FORCE ANIMATIONS (MovementStates)
            // Get the component that holds the booleans (walking, crouching, etc.)
            val stateComp = store.getComponent(dummyEntity, MovementStatesComponent.getComponentType())
            
            if (stateComp != null && input.movementStates != null) {
                stateComp.movementStates = input.movementStates
            }

            val inputComp = store.getComponent(dummyEntity, PlayerInput.getComponentType())
            
            if (inputComp != null) {
                 if (input.lookOrientation != null) {
                    inputComp.queue(PlayerInput.SetHead(input.lookOrientation))
                    inputComp.queue(PlayerInput.SetBody(input.lookOrientation))
                }
                if (input.movementStates != null) {
                    inputComp.queue(PlayerInput.SetMovementStates(input.movementStates))
                }
            }
        }
    }
}