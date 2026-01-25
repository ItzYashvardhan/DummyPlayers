package me.justlime.dummyplayer.packets

import com.hypixel.hytale.protocol.Packet
import com.hypixel.hytale.server.core.auth.PlayerAuthentication
import com.hypixel.hytale.server.core.io.ProtocolVersion
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler
import io.netty.channel.embedded.EmbeddedChannel
import java.util.UUID
import java.util.concurrent.CompletableFuture

class DummyPacketHandler : GamePacketHandler(
    EmbeddedChannel(),
    ProtocolVersion("DUMMY_HASH"),
    PlayerAuthentication(UUID.randomUUID(), "Dummy") //Fake Authentication
) {

    override fun getIdentifier(): String {
        return "DummyConnection"
    }

    // Discard outgoing packets immediately

    override fun write(vararg packets: Packet) {
        // Discard
    }

    override fun writeNoCache(packet: Packet) {
        // Discard
    }

    override fun write(packet: Packet) {
        // Discard
    }

    override fun setClientReadyForChunksFuture(future: CompletableFuture<Void>) {
        future.complete(null)
    }
}
