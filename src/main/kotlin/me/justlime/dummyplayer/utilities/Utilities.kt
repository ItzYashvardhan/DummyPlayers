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
import java.util.UUID
import java.util.concurrent.CompletableFuture

object Utilities {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun getSkinByUsername(username: String): CompletableFuture<PlayerSkin?> {
        println("[DummyPlayer] Fetching skin for '$username' via PlayerDB...")

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://playerdb.co/api/player/hytale/$username"))
            .header("User-Agent", "Hytale-Plugin/1.0")
            .GET()
            .build()

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenCompose { response ->
                if (response.statusCode() == 200) {
                    try {
                        val root = JsonParser.parseString(response.body()).asJsonObject

                        if (!root.get("success").asBoolean) {
                            println("[DummyPlayer] PlayerDB: User '$username' not found.")
                            return@thenCompose CompletableFuture.completedFuture(null)
                        }

                        val data = root.getAsJsonObject("data")
                        val player = data.getAsJsonObject("player")

                        // 1. Get UUID to check local storage (Optimization)
                        val uuidString = player.get("id").asString
                        val uuid = UUID.fromString(uuidString)

                        // 2. Check Local Storage first
                        getSkinByUuid(uuid).thenApply { localSkin ->
                            if (localSkin != null) {
                                println("[DummyPlayer] Found skin in local storage.")
                                return@thenApply localSkin
                            } else {
                                // 3. Not local? Use the skin data from PlayerDB JSON
                                println("[DummyPlayer] Parsing skin from PlayerDB JSON...")
                                return@thenApply parseSkinFromPlayerDB(player)
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        CompletableFuture.completedFuture(null)
                    }
                } else {
                    println("[DummyPlayer] PlayerDB Error: ${response.statusCode()}")
                    CompletableFuture.completedFuture(null)
                }
            }
    }

    fun getSkinByUuid(uuid: UUID): CompletableFuture<PlayerSkin?> {
        val playerStorage = Universe.get().playerStorage
        return playerStorage.load(uuid).thenApply { entityStore ->
            val skinComponent = entityStore?.getComponent(PlayerSkinComponent.getComponentType())
            skinComponent?.playerSkin
        }
    }

    private fun parseSkinFromPlayerDB(playerJson: JsonObject): PlayerSkin? {
        if (!playerJson.has("skin") || playerJson.get("skin").isJsonNull) {
            println("[DummyPlayer] Player exists but has no skin data.")
            return null
        }

        val skin = playerJson.getAsJsonObject("skin")

        // Helper to safely get string or null (handles the nulls in your JSON like "earAccessory": null)
        fun get(key: String): String? {
            return if (skin.has(key) && !skin.get(key).isJsonNull) {
                skin.get(key).asString
            } else null
        }

        // Map directly to your PlayerSkin constructor
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