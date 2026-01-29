package me.justlime.dummyplayer.pov

import com.hypixel.hytale.protocol.Direction
import com.hypixel.hytale.protocol.MovementStates
import java.util.concurrent.ConcurrentLinkedQueue

object DummyInputQueue {
    data class PendingInput(
        val dummyUuid: java.util.UUID,
        val movementStates: MovementStates?,
        val lookOrientation: Direction?
    )

    val queue = ConcurrentLinkedQueue<PendingInput>()
}