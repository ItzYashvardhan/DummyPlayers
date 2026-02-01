package me.justlime.dummyplayer.service

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.PlayerRef
import me.justlime.dummyplayer.service.DummyCameraService
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object DummyControllerService {
    // Key: Controller (Real Player), (Dummy)
    private val activeSessions = ConcurrentHashMap<UUID, UUID>()

    fun startControlling(controller: PlayerRef, dummy: PlayerRef) {
        activeSessions[controller.uuid] = dummy.uuid
        DummyCameraService.setCameraToDummy(controller, dummy)
        controller.sendMessage(Message.raw("Now controlling ${dummy.username}"))
    }

    fun stopControlling(controller: PlayerRef) {
        val dummyId = activeSessions.remove(controller.uuid)
        if (dummyId != null) {
            DummyCameraService.resetCamera(controller)
            controller.sendMessage(Message.raw("Stopped controlling."))
        }
    }

    fun toggleControlling(controller: PlayerRef, dummy: PlayerRef){
        val dummyId = activeSessions[controller.uuid]
        if (dummyId != null) {
            stopControlling(controller)
            return
        }
        startControlling(controller,dummy)
    }

    fun getControlledDummy(controllerUuid: UUID): UUID? {
        return activeSessions[controllerUuid]
    }
}