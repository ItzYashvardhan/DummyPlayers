package me.justlime.dummyplayer.network

import com.hypixel.hytale.protocol.NetworkChannel
import com.hypixel.hytale.protocol.ToClientPacket
import com.hypixel.hytale.protocol.packets.interface_.ServerMessage
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.auth.PlayerAuthentication
import com.hypixel.hytale.server.core.io.ProtocolVersion
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler
import io.netty.channel.Channel
import io.netty.channel.embedded.EmbeddedChannel
import me.justlime.dummyplayer.listener.DummyChatListener
import java.util.concurrent.CompletableFuture


class DummyPacketHandler(
    private val embeddedChannel: EmbeddedChannel,
    protocolVersion: ProtocolVersion,
    authentication: PlayerAuthentication
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
    }

    override fun write(vararg packets: ToClientPacket) {
        for (p in packets) {
            write(p)
        }
    }

    override fun writeNoCache(packet: ToClientPacket) {
        write(packet)
    }

    override fun getChannel(networkChannel: NetworkChannel): Channel {
        return embeddedChannel
    }

    override fun setClientReadyForChunksFuture(future: CompletableFuture<Void>) {
        future.complete(null)
    }
}