package me.justlime.dummyplayer.ui

import au.ellie.hyui.builders.DropdownBoxBuilder
import au.ellie.hyui.builders.HyUIPage
import au.ellie.hyui.builders.PageBuilder
import au.ellie.hyui.builders.TextFieldBuilder
import au.ellie.hyui.html.TemplateProcessor
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import me.justlime.dummyplayer.listener.DummyChatListener
import me.justlime.dummyplayer.service.DummyPlayerFactory
import me.justlime.dummyplayer.service.DummySelectorService
import java.util.Timer
import kotlin.concurrent.schedule


object UIManager {
    data class ChatLine(val text: String)

    val messages: MutableList<ChatLine> = mutableListOf()

    fun open(playerRef: PlayerRef) {
        var page: HyUIPage? = null
        val dummyNames = DummyPlayerFactory.getDummyNames()
        val template = TemplateProcessor().setVariable("messages", messages)
        val pageBuilder = PageBuilder.pageForPlayer(playerRef)
            .loadHtml("Pages/Menu.html", template)
            .enableRuntimeTemplateUpdates(true)
            .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction)


        pageBuilder.elements.forEach { elementBuilder ->
            if (elementBuilder.id == "dummy-list") {
                val dropDown = elementBuilder as DropdownBoxBuilder
                dummyNames.forEach {
                    dropDown.addEntry(it, it)
                }
                dropDown.addEventListener(CustomUIEventBindingType.ValueChanged) { it, ctx ->
                    DummySelectorService.selectDummy(playerRef.uuid, it)
                    updateUIForViewers(it)
                }
                dropDown.withValue(DummySelectorService.getSelectedDummy(playerRef.uuid) ?: "NONE")
            }
            if (elementBuilder.id == "chat-input") {
                val textField = elementBuilder as TextFieldBuilder
                textField.addEventListener(CustomUIEventBindingType.Validating) { input, ctx ->
                    val selectedDummy = DummySelectorService.getSelectedDummy(playerRef.uuid)
                    if (selectedDummy == null) {
                        playerRef.sendMessage(Message.raw("You must select a dummy"))
                        return@addEventListener
                    }
                    val selectedDummyRef = DummyPlayerFactory.getDummy(selectedDummy)
                    if (selectedDummyRef == null) {
                        playerRef.sendMessage(Message.raw("Dummy Ref not found"))
                        return@addEventListener
                    }
                    sendChat(selectedDummyRef, input)
                }
            }
        }
        val store = playerRef.reference?.store
        if (store == null) {
            playerRef.sendMessage(Message.raw("Store is null"))
            return
        }
        page = pageBuilder.open(store)

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