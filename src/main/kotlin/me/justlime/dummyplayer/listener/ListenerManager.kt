package me.justlime.dummyplayer.listener

import com.hypixel.hytale.protocol.packets.player.ClientMovement
import com.hypixel.hytale.server.core.event.events.player.*
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters
import com.hypixel.hytale.server.core.io.adapter.PacketFilter
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler
import me.justlime.dummyplayer.DummyPlayerPlugin
import me.justlime.dummyplayer.packets.DummyPacketHandler
import me.justlime.dummyplayer.pov.DummyControllerService
import me.justlime.dummyplayer.pov.DummyInputQueue

class ListenerManager(val plugin: DummyPlayerPlugin) {

    fun eventTest() {

//        ✔ -> Worked for Dummy ‼
//        ✖ -> Not worked for dummy only
//        ✖‼ -> Not worked for both Player and Dummy
//        ‼ -> Not Tested Yet

        // PlayerSetupConnectEvent (Auth) ✔
        plugin.eventRegistry.register(PlayerSetupConnectEvent::class.java) { event ->
            if (event.packetHandler is DummyPacketHandler) {
                println("[DummyEvent] PlayerSetupConnect: ${event.username} (UUID: ${event.uuid})")
            }
        }

        //  PlayerConnectEvent (World Selection) ✔
        plugin.eventRegistry.register(PlayerConnectEvent::class.java) { event ->
            if (event.playerRef.packetHandler is DummyPacketHandler) {
                println("[DummyEvent] PlayerConnect: ${event.playerRef.username} -> World: ${event.world?.name}")
            }
        }

        // AddPlayerToWorldEvent (Spawning) ✖‼
        plugin.eventRegistry.register(AddPlayerToWorldEvent::class.java) { event ->
            println("[DummyEvent] AddPlayerToWorld: ${event.world}added to ${event.world.name}")
        }

        // PlayerReadyEvent (Post-Spawn) ✖
        plugin.eventRegistry.register(PlayerReadyEvent::class.java) { event ->
            val ref = event.player.playerRef
            if (ref.packetHandler is DummyPacketHandler) {
                println("[DummyEvent] PlayerReady: ${ref.username} is ready.")
                return@register
            }
            println("[PlayerEvent] PlayerReady: ${ref.username} is ready.")
        }

        // PlayerChatEvent ✔
        plugin.eventRegistry.register(PlayerChatEvent::class.java) { event ->
            if (event.sender.packetHandler is DummyPacketHandler) {
                println("[DummyEvent] PlayerChat: ${event.sender.username} says: '${event.content}'")
            }
        }

        // PlayerInteractEvent (Interacting with blocks/entities) ‼
        plugin.eventRegistry.register(PlayerInteractEvent::class.java) { event ->
            if (event.player.playerRef.packetHandler is DummyPacketHandler) {
                println("[DummyEvent] PlayerInteract: ${event.player.playerRef.username} interacted.")
            }
        }

        // PlayerCraftEvent ‼
        plugin.eventRegistry.register(PlayerCraftEvent::class.java) { event ->
            if (event.player.playerRef.packetHandler is DummyPacketHandler) {
                println("[DummyEvent] PlayerCraft: ${event.player.playerRef.username} crafted ${event.craftedRecipe?.id}")
            }
        }

        // PlayerMouseButtonEvent (Clicks) ‼
        plugin.eventRegistry.register(PlayerMouseButtonEvent::class.java) { event ->
            if (event.player.playerRef.packetHandler is DummyPacketHandler) {
                println("[DummyEvent] PlayerMouseButton: ${event.player.playerRef.username} clicked button ${event.mouseButton}")
            }
        }

        // PlayerMouseMotionEvent (Looking around) ‼
        plugin.eventRegistry.register(PlayerMouseMotionEvent::class.java) { event ->
            if (event.player.playerRef.packetHandler is DummyPacketHandler) {
                println("[DummyEvent] PlayerMouseMotion: ${event.player.playerRef.username} moved mouse.")
            }
        }

        // DrainPlayerFromWorldEvent (Leaving World) ✖
        plugin.eventRegistry.register(DrainPlayerFromWorldEvent::class.java) { event ->
            println("[DummyEvent] DrainPlayerFromWorld: ${event.world.name}")
        }

        // PlayerSetupDisconnectEvent (Pre-Disconnect) ‼
        plugin.eventRegistry.register(PlayerSetupDisconnectEvent::class.java) { event ->
            println("[DummyEvent] PlayerSetupDisconnect: ${event.username} handler disconnecting. Reason: ${event.disconnectReason}")
        }

        // PlayerDisconnectEvent (Full Disconnect) ✔
        plugin.eventRegistry.register(PlayerDisconnectEvent::class.java) { event ->
            if (event.playerRef.packetHandler is DummyPacketHandler) {
                println("[DummyEvent] PlayerDisconnect: ${event.playerRef.username} disconnected. Reason: ${event.disconnectReason}")
            }
        }

        // PlayerRefEvent (Base class for Ref events)
        plugin.eventRegistry.register(PlayerRefEvent::class.java) { event ->
            if (event.playerRef.packetHandler is DummyPacketHandler) {
//                 println("[DummyEvent] Generic PlayerRefEvent: ${event.javaClass.simpleName}")
            }
        }
    }


    fun register() {


        PacketAdapters.registerInbound(PacketFilter { handler, packet ->

            if (handler !is GamePacketHandler) return@PacketFilter false
            if (packet !is ClientMovement) return@PacketFilter false

            val controller = handler.playerRef
            val dummyUuid = DummyControllerService.getControlledDummy(controller.uuid) ?: return@PacketFilter false

            val states = packet.movementStates?.clone()
            val look = packet.lookOrientation
            DummyInputQueue.queue.add(DummyInputQueue.PendingInput(dummyUuid, states, look))
            return@PacketFilter true
        })
    }
}

