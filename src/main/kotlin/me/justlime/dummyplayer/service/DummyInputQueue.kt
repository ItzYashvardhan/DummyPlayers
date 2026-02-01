package me.justlime.dummyplayer.service

import com.hypixel.hytale.protocol.Direction
import com.hypixel.hytale.protocol.MovementStates
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object DummyInputQueue {
    data class PendingInput(
        val dummyUuid: UUID,
        val movementStates: MovementStates?,
        val lookOrientation: Direction?
    )
    val queue = ConcurrentLinkedQueue<PendingInput>()
}