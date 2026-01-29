package me.justlime.dummyplayer.utilities

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hypixel.hytale.protocol.PlayerSkin
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.universe.Universe
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture

object Utilities {

    // HTTP Client with Timeouts
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    /**
     * Standard Fetch.
     */
    fun getSkin(username: String): CompletableFuture<PlayerSkin?> {
        return fetchSkinInternal(username, forceApi = false)
    }

    /**
     * Force Refresh.
     */
    fun refreshSkin(username: String): CompletableFuture<PlayerSkin?> {
        return fetchSkinInternal(username, forceApi = true)
    }

    /**
     * Internal logic to handle the API call and decision matrix.
     */
    private fun fetchSkinInternal(username: String, forceApi: Boolean): CompletableFuture<PlayerSkin?> {
        if (username.isBlank()) return CompletableFuture.completedFuture(null)

        println("[DummyPlayer] ${if(forceApi) "Refreshing" else "Fetching"} skin for '$username'...")

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://playerdb.co/api/player/hytale/$username"))
            .header("User-Agent", "Hytale-Plugin-DummyPlayer/1.0")
            .timeout(Duration.ofSeconds(5)) // Request Timeout
            .GET()
            .build()

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenCompose { response ->
                if (response.statusCode() != 200) {
                    println("[DummyPlayer] API Error: ${response.statusCode()}")
                    return@thenCompose CompletableFuture.completedFuture(null)
                }

                try {
                    val root = JsonParser.parseString(response.body()).asJsonObject

                    if (!root.get("success").asBoolean) {
                        println("[DummyPlayer] User '$username' not found in PlayerDB.")
                        return@thenCompose CompletableFuture.completedFuture(null)
                    }

                    val data = root.getAsJsonObject("data")
                    val player = data.getAsJsonObject("player")

                    // 1. Parse API Data
                    val uuid = UUID.fromString(player.get("id").asString)
                    val apiSkin = parseSkinFromPlayerDB(player)


                    if (forceApi) {
                        // Use API skin if valid, fall back to local only if API failed parsing
                        if (apiSkin != null) {
                            println("[DummyPlayer] Refreshed skin from API.")
                            return@thenCompose CompletableFuture.completedFuture(apiSkin)
                        }
                    } else {
                        // Check Local Storage first
                        return@thenCompose getSkinByUuid(uuid).thenApply { localSkin ->
                            if (localSkin != null) {
                                println("[DummyPlayer] Found local skin, ignoring API.")
                                return@thenApply localSkin
                            } else {
                                return@thenApply apiSkin
                            }
                        }
                    }

                    // Force refresh but API json missing skin
                    return@thenCompose getSkinByUuid(uuid)

                } catch (e: Exception) {
                    println("[DummyPlayer] Error parsing skin: ${e.message}")
                    return@thenCompose CompletableFuture.completedFuture(null)
                }
            }
            // Timeouts / No Internet
            .exceptionally { e ->
                println("[DummyPlayer] Network failed for '$username': ${e.cause?.message ?: e.message}")
                null
            }
    }

    /**
     * Attempts to load the skin from Hytale's internal disk storage.
     */
    fun getSkinByUuid(uuid: UUID): CompletableFuture<PlayerSkin?> {
        val playerStorage = Universe.get().playerStorage

        return playerStorage.load(uuid)
            .thenApply { entityStore ->
                if (entityStore == null) return@thenApply null

                val skinComponent = entityStore.getComponent(PlayerSkinComponent.getComponentType())
                skinComponent?.playerSkin
            }
            .exceptionally {
                println("[DummyPlayer] Disk load failed: ${it.message}")
                null
            }
    }

    /**
     * JSON Parser
     */
    private fun parseSkinFromPlayerDB(playerJson: JsonObject): PlayerSkin? {
        if (!playerJson.has("skin") || playerJson.get("skin").isJsonNull) {
            return null
        }

        val skin = playerJson.getAsJsonObject("skin")

        fun get(key: String): String? {
            val element = skin.get(key)
            return if (element != null && !element.isJsonNull) element.asString else null
        }

        return PlayerSkin(
            get("bodyCharacteristic"),
            get("underwear"),
            get("face"),
            get("eyes"),
            get("ears"),
            get("mouth"),
            get("facialHair"),
            get("haircut"),
            get("eyebrows"),
            get("pants"),
            get("overpants"),
            get("undertop"),
            get("overtop"),
            get("shoes"),
            get("headAccessory"),
            get("faceAccessory"),
            get("earAccessory"),
            get("skinFeature"),
            get("gloves"),
            get("cape")
        )
    }
}