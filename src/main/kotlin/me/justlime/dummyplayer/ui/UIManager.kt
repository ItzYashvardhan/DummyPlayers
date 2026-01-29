package me.justlime.dummyplayer.ui

import au.ellie.hyui.builders.ButtonBuilder
import au.ellie.hyui.builders.DropdownBoxBuilder
import au.ellie.hyui.builders.PageBuilder
import au.ellie.hyui.builders.TextFieldBuilder
import au.ellie.hyui.html.TemplateProcessor
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import me.justlime.dummyplayer.listener.DummyChatListener
import me.justlime.dummyplayer.service.DummyPlayerFactory
import me.justlime.dummyplayer.service.DummySelectorService
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object UIManager {
    data class ChatLine(val text: String)

    // Maps Player UUID -> List of ChatLines for their UI view
    private val playerChatLogs = ConcurrentHashMap<UUID, MutableList<ChatLine>>()

    fun open(playerRef: PlayerRef) {
        val playerUuid = playerRef.uuid
        val selectedDummy = DummySelectorService.getSelectedDummy(playerUuid)

        // Synchronize UI logs with the actual dummy log before building
        if (selectedDummy != null) {
            updatePlayerLogs(playerUuid, selectedDummy)
        }

        val dummyNames = DummyPlayerFactory.getDummyNames()
        val template = TemplateProcessor().setVariable("messages", playerChatLogs[playerUuid] ?: emptyList<ChatLine>())

        val pageBuilder = PageBuilder.pageForPlayer(playerRef).loadHtml("Pages/Menu.html", template)

        pageBuilder.elements.forEach { elementBuilder ->
            // Dummy Selector
            if (elementBuilder.id == "dummy-list") {
                val dropDown = elementBuilder as DropdownBoxBuilder
                dummyNames.forEach { dropDown.addEntry(it, it) }

                dropDown.addEventListener(CustomUIEventBindingType.ValueChanged) { value, _ ->
                    DummySelectorService.selectDummy(playerUuid, value)
                    reopen(playerRef)
                }
                dropDown.withValue(selectedDummy ?: "NONE")
            }

            // Chat Input
            if (elementBuilder.id == "chat-input") {
                val textField = elementBuilder as TextFieldBuilder
                textField.addEventListener(CustomUIEventBindingType.Validating) { input, _ ->
                    val currentDummy = DummySelectorService.getSelectedDummy(playerUuid)
                    if (currentDummy == null) {
                        playerRef.sendMessage(Message.raw("You must select a dummy"))
                        return@addEventListener
                    }

                    val dummyRef = DummyPlayerFactory.getDummy(currentDummy)
                    if (dummyRef == null) {
                        playerRef.sendMessage(Message.raw("Dummy Ref not found"))
                        return@addEventListener
                    }

                    sendChat(dummyRef, input) {
                        reopen(playerRef)
                    }
                }
            }

            // Pov Button
            if (elementBuilder.id == "pov-btn") {
                val button = elementBuilder as ButtonBuilder
                button.addEventListener(CustomUIEventBindingType.Activating) { void, context ->
                    val currentDummy = DummySelectorService.getSelectedDummy(playerUuid)
                    if (currentDummy == null) {
                        playerRef.sendMessage(Message.raw("You must select a dummy"))
                        return@addEventListener
                    }
                    val dummyRef = DummyPlayerFactory.getDummy(currentDummy)
                    if (dummyRef == null) {
                        playerRef.sendMessage(Message.raw("Dummy Ref not found"))
                        return@addEventListener
                    }
//                    DummyControllerService.toggleControlling(playerRef, dummyRef)
                    context.page.get().close()
                }
            }
        }

        playerRef.reference?.store?.let { pageBuilder.open(it) }
            ?: playerRef.sendMessage(Message.raw("Store is null"))
    }

    /**
     * Updates the specific log for a player based on the dummy they are watching.
     */
    private fun updatePlayerLogs(playerUuid: UUID, dummyName: String) {
        val newLog = DummyChatListener.getLogForUI(dummyName)
        val logs = playerChatLogs.computeIfAbsent(playerUuid) { mutableListOf() }

        logs.clear()
        logs.addAll(newLog.map { ChatLine(it.ansiMessage) })
    }

    private fun reopen(playerRef: PlayerRef) {
        val worldUuid = playerRef.worldUuid ?: run {
            playerRef.sendMessage(Message.raw("Player not in a world!"))
            return
        }

        val world = Universe.get().getWorld(worldUuid) ?: run {
            playerRef.sendMessage(Message.raw("World not found!"))
            return
        }

        world.execute { open(playerRef) }
    }

    private fun sendChat(playerRef: PlayerRef, input: String, onFinished: () -> Unit = {}) {
        if (input.isBlank()) {
            onFinished()
            return
        }

        if (!playerRef.isValid) {
            playerRef.sendMessage(Message.raw("reference not found or invalid entity"))
            return
        }
        val store = playerRef.reference?.store
        if (store == null) {
            playerRef.sendMessage(Message.raw("store not found"))
            return
        }
        val world = store.externalData.world
        world.execute {
            val future: CompletableFuture<Void> = when (input.first()) {
                '/' -> {
                    HytaleServer.get().commandManager.handleCommand(playerRef, input.removePrefix("/"))
                }

                else -> {
                    playerRef.chat(input)
                    CompletableFuture.completedFuture(null)
                }
            }
            future.whenComplete { _, exception ->
                if (exception != null) {
                    println("Command failed for ${playerRef.username}: ${exception.message}")
                }
                onFinished()
            }
        }
    }

    private fun PlayerRef.chat(text: String) {
        this.packetHandler.handle(ChatMessage(text))
    }

    fun cleanup(playerUuid: UUID) {
        playerChatLogs.remove(playerUuid)
    }
}