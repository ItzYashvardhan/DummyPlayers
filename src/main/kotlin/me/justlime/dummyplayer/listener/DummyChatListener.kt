package me.justlime.dummyplayer.listener

import com.hypixel.hytale.server.core.Message
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object DummyChatListener {
    private const val MESSAGE_LIMIT = 18

    // Stores the last 18 messages for each dummy (Key: DummyName, Messages)
    private val chatHistory = ConcurrentHashMap<String, MutableList<Message>>()

    @ApiStatus.Internal
    fun onPacketSend(targetPlayerName: String, message: Message) {
        // Save to History
        val history = chatHistory.computeIfAbsent(targetPlayerName) {
            Collections.synchronizedList(ArrayList())
        }

        synchronized(history) {
            if (history.size >= MESSAGE_LIMIT) history.removeAt(0) // Keep limit
            history.add(message)
        }
    }

    /**
     * Returns the full chat history
     */
    fun getLogForUI(dummyName: String): List<Message> {
        return chatHistory[dummyName] ?: emptyList()
    }

}