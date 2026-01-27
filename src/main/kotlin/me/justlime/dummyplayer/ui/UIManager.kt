package me.justlime.dummyplayer.ui

import au.ellie.hyui.builders.DropdownBoxBuilder
import au.ellie.hyui.builders.PageBuilder
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.PlayerRef
import me.justlime.dummyplayer.service.DummyPlayerFactory
import me.justlime.dummyplayer.service.DummySelectorService
import me.justlime.dummyplayer.utilities.ResourceLoader
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object UIManager {

    val playersDummyUI = ConcurrentHashMap<UUID, PageBuilder>()

    fun open(playerRef: PlayerRef) {
        val menuContentHtml = ResourceLoader.menuContentHtml
        val dummyNames = DummyPlayerFactory.getDummyNames()
        val builder = PageBuilder.pageForPlayer(playerRef).fromHtml(menuContentHtml)
        builder.elements.forEach { elementBuilder ->
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
                }
                dropDown.withValue(DummySelectorService.getSelectedDummy(playerRef.uuid) ?: "NONE")
            }
        }
        playersDummyUI[playerRef.uuid] = builder
        val store = playerRef.reference?.store
        if (store == null) {
            playerRef.sendMessage(Message.raw("Store is null"))
            return
        }
        builder.open(store)
    }
}