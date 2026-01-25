package me.justlime.dummyplayer.ai

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.behaviour.DummyBehavior
import kotlin.math.atan2
import kotlin.math.sqrt

class FollowBehavior(private val targetName: String) : me.justlime.dummyplayer.behaviour.DummyBehavior {
    override fun tick(world: World, dummyRef: Ref<EntityStore>, dummyName: String) {
        val store = world.entityStore.store

        // Find target player
        var targetRef: Ref<EntityStore>? = null
        for (pRef in world.playerRefs) {
            if (pRef.username.equals(targetName, ignoreCase = true)) {
                targetRef = pRef.reference
                break
            }
        }

        if (targetRef != null && targetRef.isValid) {
            val dummyTransform = store.getComponent(dummyRef, TransformComponent.getComponentType())
            val targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType())

            if (dummyTransform != null && targetTransform != null) {
                val dummyPos = dummyTransform.position
                val targetPos = targetTransform.position

                val dx = targetPos.x - dummyPos.x
                val dz = targetPos.z - dummyPos.z
                val distance = sqrt(dx * dx + dz * dz)

                if (distance > 2.0) {
                    val speed = 0.15
                    val moveX = (dx / distance) * speed
                    val moveZ = (dz / distance) * speed

                    val newPos = Vector3d(dummyPos.x + moveX, dummyPos.y, dummyPos.z + moveZ)

                    // Calculate rotation in degrees (Hytale/Minecraft usually use degrees)
                    val yawRadians = -atan2(dx, dz)
                    val yawDegrees = Math.toDegrees(yawRadians).toFloat()
                    val rotation = Vector3f(0f, yawDegrees, 0f)

                    store.putComponent(dummyRef, TransformComponent.getComponentType(), TransformComponent(newPos, rotation))
                    store.putComponent(dummyRef, HeadRotation.getComponentType(), HeadRotation(rotation))

                    val moveStates = store.getComponent(dummyRef, MovementStatesComponent.getComponentType())
                    if (moveStates != null) {
                        if (moveStates.movementStates.idle) {
                            moveStates.movementStates.idle = false
                            moveStates.movementStates.walking = true
                            store.putComponent(dummyRef, MovementStatesComponent.getComponentType(), moveStates)
                        }
                    }
                } else {
                    val moveStates = store.getComponent(dummyRef, MovementStatesComponent.getComponentType())
                    if (moveStates != null) {
                        if (!moveStates.movementStates.idle) {
                            moveStates.movementStates.idle = true
                            moveStates.movementStates.walking = false
                            store.putComponent(dummyRef, MovementStatesComponent.getComponentType(), moveStates)
                        }
                    }
                }
            }
        }
    }
}