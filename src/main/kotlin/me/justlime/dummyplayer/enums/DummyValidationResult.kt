package me.justlime.dummyplayer.enums
import com.hypixel.hytale.server.core.universe.PlayerRef

sealed interface DummyValidationResult {

    /**
     * Success! The dummy is now in the world.
     */
    data class Success(
        val playerRef: PlayerRef,
        val dummyRef: PlayerRef
    ) : DummyValidationResult

    /**
     * Failed: A dummy with this name is already online.
     * Contains the reference to the existing dummy.
     */
    data class AlreadyExists(
        val playerRef: PlayerRef
    ) : DummyValidationResult

    /**
     * Failed: A plugin (like a Ban plugin or Whitelist) cancelled the join event.
     */
    data class ConnectionDenied(
        val reason: String
    ) : DummyValidationResult

    /**
     * Failed: Internal server error (World unloading, Database error, etc).
     */
    data class Failure(
        val message: String,
        val exception: Throwable? = null
    ) : DummyValidationResult
}