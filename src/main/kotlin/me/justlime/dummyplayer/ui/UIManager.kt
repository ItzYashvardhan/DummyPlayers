package me.justlime.dummyplayer.ui

import au.ellie.hyui.builders.DropdownBoxBuilder
import au.ellie.hyui.builders.PageBuilder
import au.ellie.hyui.html.TemplateProcessor
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.PlayerRef
import me.justlime.dummyplayer.listener.DummyChatListener
import me.justlime.dummyplayer.service.DummyPlayerFactory
import me.justlime.dummyplayer.service.DummySelectorService


object UIManager {
    data class ChatLine(val text: String)

    val messages: MutableList<ChatLine> = mutableListOf()

    fun open(playerRef: PlayerRef) {
        val dummyNames = DummyPlayerFactory.getDummyNames()
        val template = TemplateProcessor().setVariable("messages", messages)
        val pageBuilder = PageBuilder.pageForPlayer(playerRef).loadHtml("Pages/Menu.html", template)

        pageBuilder.elements.forEach { elementBuilder ->
            if (elementBuilder.id == "dummy-list") {
                val dropDown = elementBuilder as DropdownBoxBuilder
                dummyNames.forEach {
                    dropDown.addEntry(it.uppercase(), it)
                }
                dropDown.addEventListener(CustomUIEventBindingType.ValueChanged) {
                    DummySelectorService.selectDummy(playerRef.uuid, it)
                    updateUIForViewers(it)
                }
                dropDown.withValue(DummySelectorService.getSelectedDummy(playerRef.uuid) ?: "NONE")
            }
        }
        val store = playerRef.reference?.store
        if (store == null) {
            playerRef.sendMessage(Message.raw("Store is null"))
            return
        }
        pageBuilder.addEventListener("chat-input", CustomUIEventBindingType.Validating) { _, context ->
            context.getValue("chat-input", String::class.java).ifPresent { message ->
                if (message.isNotBlank()) {
                    sendChat(playerRef, message)
                }
            }
        }
        pageBuilder.open(store)
    }


    fun updateUIForViewers(dummyName: String) {
        val newLog = DummyChatListener.getLogForUI(dummyName)
        messages.clear()
        messages.addAll(newLog.map { ChatLine(it.ansiMessage) })
    }


    private fun sendChat(playerRef: PlayerRef, input: String) {
        if (input.isBlank()) return
        when (input.first()) {
            '.' -> playerRef.chat(input.removePrefix(".")) //TODO
            '/' -> HytaleServer.get().commandManager.handleCommand(playerRef, input.removePrefix("/"))
            else -> playerRef.chat(input)
        }
    }

    private fun PlayerRef.chat(text: String) {
        val packet = ChatMessage(text)
        val handler = this.packetHandler
        handler.handle(packet)
    }
}