package me.justlime.dummyplayer.listener

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.Universe
import me.justlime.dummyplayer.service.DummySelectorService
import me.justlime.dummyplayer.ui.UIManager
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object DummyChatListener {
    private const val MESSAGE_LIMIT = 15

    // Stores the last 15 messages for each dummy (Key: DummyName, Messages)
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

        updateUIForViewers(targetPlayerName)
    }

    /**
     * Returns the full chat history
     */
    fun getLogForUI(dummyName: String): List<Message> {
        return chatHistory[dummyName] ?: emptyList()
    }

    private fun updateUIForViewers(dummyName: String) {
        val viewers = DummySelectorService.getPlayersWatchingDummy(dummyName.uppercase())
        println("$viewers $dummyName")
        for (viewerId in viewers) {
            val uiPair = UIManager.playersDummyUI[viewerId] ?: continue
            val page = uiPair.first
            val template = uiPair.second
            val newLog = getLogForUI(dummyName)
            newLog.forEach {
                val player = Universe.get().getPlayer(viewerId)
                if (player == null) {
                    println("PlayerNotFound")
                    return@forEach
                }
                val rawText = it.ansiMessage
                player.sendMessage(Message.raw("[$dummyName] $rawText"))
            }
            // Update the template variable
            template.setVariable("chat_messages", newLog.joinToString { "<p>${it.ansiMessage}</p>" })
            page.updatePage(false)
        }
    }
}