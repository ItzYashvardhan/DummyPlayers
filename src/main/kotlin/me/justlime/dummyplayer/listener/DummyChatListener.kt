package me.justlime.dummyplayer.listener

import au.ellie.hyui.builders.LabelBuilder
import com.hypixel.hytale.protocol.packets.interface_.ChatType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.Universe
import me.justlime.dummyplayer.service.DummySelectorService
import me.justlime.dummyplayer.ui.UIManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object DummyChatListener {
    private const val MESSAGE_LIMIT = 15

    // Stores the last 15 messages for each dummy (Key: DummyName, Messages)
    private val chatHistory = ConcurrentHashMap<String, MutableList<Message>>()

    /**
     * Call this method when your packet interceptor catches an OUTGOING ChatMessage
     */
    fun onPacketSend(targetPlayerName: String, chatType: ChatType, message: Message) {
        // Save to History
        val history = chatHistory.computeIfAbsent(targetPlayerName) {
            Collections.synchronizedList(ArrayList())
        }

        synchronized(history) {
            if (history.size >= MESSAGE_LIMIT) history.removeAt(0) // Keep limit
            history.add(message)
        }

        updateUIForViewers(targetPlayerName)
    }

    /**
     * Returns the full chat history
     */
    fun getLogForUI(dummyName: String): List<Message> {
        return chatHistory[dummyName] ?: emptyList()
    }

    private fun updateUIForViewers(dummyName: String) {
        val viewers = DummySelectorService.getPlayersWatchingDummy(dummyName)
        for (viewer in viewers) {
            val builder = UIManager.playersDummyUI[viewer] ?: return
            builder.elements.forEach { elementBuilder ->
                if (elementBuilder.id == "chat-messages") {
                    val newLog = getLogForUI(dummyName)
                    val containerBuilder = LabelBuilder.label().withText("${newLog.size} messages")
                    Universe.get().getPlayer(viewer)?.let {ref ->
                        newLog.forEach {
                            ref.sendMessage(it)
                        }
                    }
                    elementBuilder.addChild(containerBuilder)
                }
            }
        }
    }
}