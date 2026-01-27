package me.justlime.dummyplayer.service

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object DummySelectorService {
    private val sessions = ConcurrentHashMap<UUID, String>() //Player UUID and their selected Dummy Name

    fun selectDummy(playerViewerId: UUID, dummyName: String) {
        sessions.remove(playerViewerId)
        sessions[playerViewerId] = dummyName
    }

    fun deselectDummy(playerViewerId: UUID) {
        sessions.remove(playerViewerId)
    }

    fun getSelectedDummy(playerViewerId: UUID): String? {
        return sessions[playerViewerId]
    }

    fun getPlayersWatchingDummy(dummyName: String): List<UUID> {
        return sessions.filterValues { it == dummyName }.keys.toList()
    }

    fun getSessions(): ConcurrentHashMap<UUID, String> {
        return sessions
    }


}