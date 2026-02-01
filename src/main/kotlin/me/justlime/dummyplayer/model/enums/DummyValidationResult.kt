package me.justlime.dummyplayer.model.enums

import com.hypixel.hytale.server.core.universe.PlayerRef

sealed interface DummyValidationResult {
    data class Success(val playerRef: PlayerRef, val dummyRef: PlayerRef) : DummyValidationResult
    data class AlreadyExists(val playerRef: PlayerRef) : DummyValidationResult
    data class ConnectionDenied(val reason: String) : DummyValidationResult
    data class Failure(val message: String, val exception: Throwable? = null) : DummyValidationResult
}