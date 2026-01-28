package me.justlime.dummyplayer.ui

import au.ellie.hyui.builders.DropdownBoxBuilder
import au.ellie.hyui.builders.HyUIPage
import au.ellie.hyui.builders.PageBuilder
import au.ellie.hyui.html.TemplateProcessor
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.PlayerRef
import me.justlime.dummyplayer.service.DummyPlayerFactory
import me.justlime.dummyplayer.service.DummySelectorService
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object UIManager {

    val playersDummyUI = ConcurrentHashMap<UUID, Pair<HyUIPage, TemplateProcessor>>()

    fun open(playerRef: PlayerRef) {
        val dummyNames = DummyPlayerFactory.getDummyNames()

        val template = TemplateProcessor().setVariable("chat_messages", "<p>No Messages Yet...</p>")

        val pageBuilder = PageBuilder.pageForPlayer(playerRef)
            .loadHtml("Pages/Menu.html")

        pageBuilder.elements.forEach { elementBuilder ->
            if (elementBuilder.id == "dummy-list") {
                val dropDown = elementBuilder as DropdownBoxBuilder
                dummyNames.forEach {
                    dropDown.addEntry(it.uppercase(), it)
                }
                dropDown.addEventListener(CustomUIEventBindingType.ValueChanged) {
                    DummySelectorService.selectDummy(playerRef.uuid, it)
                    playerRef.sendMessage(
                        Message.raw(
                            "Selected Dummy: ${
                                DummySelectorService.getSelectedDummy(playerRef.uuid) ?: "NONE"
                            }"
                        )
                    )
                    playerRef.sendMessage(Message.raw(DummySelectorService.getSessions().toString()))
                }
                dropDown.withValue(DummySelectorService.getSelectedDummy(playerRef.uuid) ?: "NONE")
            }
        }
        val store = playerRef.reference?.store
        if (store == null) {
            playerRef.sendMessage(Message.raw("Store is null"))
            return
        }
        val page = pageBuilder.open(store).page.get()
        playersDummyUI[playerRef.uuid] = Pair(page, template)

    }
}