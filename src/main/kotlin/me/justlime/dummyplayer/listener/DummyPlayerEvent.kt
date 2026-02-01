package me.justlime.dummyplayer.listener

import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.auth.PlayerAuthentication
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.network.DummyPacketHandler
import java.util.*

object DummyPlayerEvent {

    fun setupConnect(
        handler: DummyPacketHandler,
        username: String,
        uuid: UUID,
        auth: PlayerAuthentication
    ): PlayerSetupConnectEvent? {
        return HytaleServer.get().eventBus
            .dispatchFor(PlayerSetupConnectEvent::class.java)
            .dispatch(PlayerSetupConnectEvent(handler, username, uuid, auth, null, null))
    }


    fun connect(holder: Holder<EntityStore>, playerRef: PlayerRef, intendedWorld: World): World {
        val event = HytaleServer.get().eventBus
            .dispatchFor(PlayerConnectEvent::class.java)
            .dispatch(
                PlayerConnectEvent(holder, playerRef, intendedWorld)
            ) as PlayerConnectEvent
        return event.world ?: Universe.get().defaultWorld ?: intendedWorld
    }


    fun disconnect(playerRef: PlayerRef) {
        HytaleServer.get().eventBus
            .dispatchFor(PlayerDisconnectEvent::class.java)
            .dispatch(PlayerDisconnectEvent(playerRef))
    }
}