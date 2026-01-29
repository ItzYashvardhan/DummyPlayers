package me.justlime.dummyplayer.pov.camera

import com.hypixel.hytale.protocol.AttachedToType
import com.hypixel.hytale.protocol.CanMoveType
import com.hypixel.hytale.protocol.ClientCameraView
import com.hypixel.hytale.protocol.MovementStates
import com.hypixel.hytale.protocol.ServerCameraSettings
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.CameraManager
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.entity.movement.MovementStatesSystems
import com.hypixel.hytale.server.core.universe.PlayerRef

object DummyCameraService {


    fun setCameraToDummy(controller: PlayerRef, dummy: PlayerRef) {


        val settings = ServerCameraSettings()
        val dummyEntityRef = dummy.reference
        if (dummyEntityRef == null) {
            controller.sendMessage(Message.raw("Dummy is not currently in the world!"))
            return
        }
        val dummyStore = dummyEntityRef.store
        val player = dummyStore.getComponent(dummyEntityRef, Player.getComponentType())
        if (player == null) {
            controller.sendMessage(Message.raw("Dummy is not a player!"))
            return
        }
        val cameraManager = dummyStore.getComponent(dummyEntityRef, CameraManager.getComponentType())
        if (cameraManager == null) {
            controller.sendMessage(Message.raw("Dummy has no camera manager!"))
            return
        }



        settings.attachedToType = AttachedToType.EntityId
        settings.attachedToEntityId = player.networkId

        // Configure POV (First Person)
        settings.isFirstPerson = true
        settings.distance = 0.0f

        // Input Configuration
        settings.allowPitchControls = true
        settings.sendMouseMotion = true
        settings.eyeOffset = true
        settings.skipCharacterPhysics = true

        settings.canMoveType = CanMoveType.Always

        // Send the Packet
        val packet = SetServerCamera(ClientCameraView.Custom, false, settings)
        controller.packetHandler.write(packet)
    }

    fun resetCamera(controller: PlayerRef) {
        val packet = SetServerCamera(ClientCameraView.Custom, false, null)
        controller.packetHandler.write(packet)
    }
}