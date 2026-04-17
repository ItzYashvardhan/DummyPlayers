package me.justlime.dummyplayer.network

import com.hypixel.hytale.protocol.ToClientPacket
import com.hypixel.hytale.protocol.packets.connection.Ping
import com.hypixel.hytale.protocol.packets.connection.Pong
import com.hypixel.hytale.protocol.packets.connection.PongType
import com.hypixel.hytale.protocol.packets.interface_.ServerMessage
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.auth.PlayerAuthentication
import com.hypixel.hytale.server.core.io.ProtocolVersion
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource
import io.netty.channel.embedded.EmbeddedChannel
import me.justlime.dummyplayer.listener.DummyChatListener
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


class DummyPacketHandler(
    val embeddedChannel: EmbeddedChannel,
    protocolVersion: ProtocolVersion,
    authentication: PlayerAuthentication,
    val simulatedPingMs: Long,
) : GamePacketHandler(embeddedChannel, protocolVersion, authentication) {
    override fun getIdentifier(): String {
        return "DummyConnection"
    }

    override fun write(packet: ToClientPacket) {
        if (packet is ServerMessage) {
            val dummyName = this.auth?.username ?: return
            val formattedMessage = packet.message ?: return
            val message = Message(formattedMessage)
            DummyChatListener.onPacketSend(dummyName, message)
            return
        }
        if (packet is Ping) {
            val instantData = WorldTimeResource.instantToInstantData(Instant.now())
            embeddedChannel.writeInbound(Pong(packet.id, instantData, PongType.Raw, 0.toShort()))
            embeddedChannel.writeInbound(Pong(packet.id, instantData, PongType.Direct, 0.toShort()))
            if (simulatedPingMs > 0) {
                embeddedChannel.eventLoop().schedule({
                    val tickInstantData = WorldTimeResource.instantToInstantData(Instant.now())
                    embeddedChannel.writeInbound(Pong(packet.id, tickInstantData, PongType.Tick, 0.toShort()))
                }, simulatedPingMs, TimeUnit.MILLISECONDS)
            } else {
                embeddedChannel.writeInbound(Pong(packet.id, instantData, PongType.Tick, 0.toShort()))
            }
            return
        }
    }

    override fun write(vararg packets: ToClientPacket) {
        for (p in packets) {
            write(p)
        }
    }

    override fun writeNoCache(packet: ToClientPacket) {
        write(packet)
    }

    override fun writePacket(packet: ToClientPacket, cache: Boolean) {
        write(packet)
    }

    override fun setClientReadyForChunksFuture(future: CompletableFuture<Void>) {
        future.complete(null)
    }
}