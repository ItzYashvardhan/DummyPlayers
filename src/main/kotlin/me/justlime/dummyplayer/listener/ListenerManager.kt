package me.justlime.dummyplayer.listener

import com.hypixel.hytale.protocol.packets.player.ClientMovement
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters
import com.hypixel.hytale.server.core.io.adapter.PacketFilter
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler
import me.justlime.dummyplayer.pov.DummyControllerService
import me.justlime.dummyplayer.pov.DummyInputQueue

class ListenerManager {
    fun register() {
        PacketAdapters.registerInbound(PacketFilter { handler, packet ->

            if (handler !is GamePacketHandler) return@PacketFilter false
            if (packet !is ClientMovement) return@PacketFilter false

            val controller = handler.playerRef
            val dummyUuid = DummyControllerService.getControlledDummy(controller.uuid) ?: return@PacketFilter false

            val states = packet.movementStates?.clone()
            val look = packet.lookOrientation
            DummyInputQueue.queue.add(DummyInputQueue.PendingInput(dummyUuid, states, look))
            return@PacketFilter true
        })
    }
}

