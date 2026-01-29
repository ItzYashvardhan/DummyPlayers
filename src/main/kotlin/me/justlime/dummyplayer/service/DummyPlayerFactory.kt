package me.justlime.dummyplayer.service

import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.protocol.PlayerSkin
import com.hypixel.hytale.protocol.ProtocolSettings
import com.hypixel.hytale.protocol.packets.interface_.AddToServerPlayerList
import com.hypixel.hytale.protocol.packets.interface_.RemoveFromServerPlayerList
import com.hypixel.hytale.protocol.packets.interface_.ServerPlayerListPlayer
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.auth.PlayerAuthentication
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
import com.hypixel.hytale.server.core.entity.UUIDComponent
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.io.ProtocolVersion
import com.hypixel.hytale.server.core.modules.entity.component.*
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues
import com.hypixel.hytale.server.core.modules.physics.component.Velocity
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import io.netty.channel.embedded.EmbeddedChannel
import me.justlime.dummyplayer.DummyPlayerPlugin
import me.justlime.dummyplayer.ecs.DummyComponent
import me.justlime.dummyplayer.enums.DummyValidationResult
import me.justlime.dummyplayer.packets.DummyPacketHandler
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object DummyPlayerFactory {

    //    private val cloneCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val dummies = ConcurrentHashMap<UUID, PlayerRef>()

    // ===========================================================================================
    // SPAWNING
    // ===========================================================================================

    fun spawnDummy(
        world: World,
        username: String,
        position: Vector3d,
        skin: PlayerSkin? = null
    ): CompletableFuture<DummyValidationResult> {
        val uuid = getDummyUUID(username)

        val existingRef = dummies[uuid]
        if (existingRef != null) {
            if (existingRef.isValid) {
                return CompletableFuture.completedFuture(DummyValidationResult.AlreadyExists(existingRef))
            }
            dummies.remove(uuid)
        }

        // Core Network Objects
        val holder = EntityStore.REGISTRY.newHolder()
        val embeddedChannel = EmbeddedChannel()
        val protocolVersion = ProtocolVersion(ProtocolSettings.PROTOCOL_VERSION)
        val authentication = PlayerAuthentication(uuid, username)
        val dummyHandler = DummyPacketHandler(embeddedChannel, protocolVersion, authentication)
        val event = DummyPlayerEvent.setupConnect(dummyHandler, username, uuid, authentication)
        if (event != null && event.isCancelled) {
            val reason = event.reason ?: "Connection rejected."
            return CompletableFuture.completedFuture(DummyValidationResult.ConnectionDenied(reason))
        }

        // Setup Entity
        val chunkTracker = ChunkTracker()
        val dummyRef = PlayerRef(holder, uuid, username, "en_US", dummyHandler, chunkTracker)
        setupHolderComponents(holder, dummyRef, chunkTracker, uuid, skin, dummyHandler, username)

        // Add to World
        modifyUniversePlayersMap { it[uuid] = dummyRef }
        val spawnPos = Vector3d(position.x, position.y + 1.0, position.z)
        val spawnTransform = Transform(spawnPos)

        // Initialize Transform component
        holder.addComponent(TransformComponent.getComponentType(), TransformComponent(spawnPos, Vector3f(0f, 0f, 0f)))
        val result: CompletableFuture<DummyValidationResult> =
            world.addPlayer(dummyRef, spawnTransform)?.thenApply { ref ->
                if (ref != null && ref.isValid) {
                    dummies[uuid] = dummyRef
                    tabListAddPlayer(world, uuid, username)
                    DummyValidationResult.Success(dummyRef, dummyRef)
                } else DummyValidationResult.Failure("Invalid dummy reference")
            } ?: CompletableFuture.completedFuture(DummyValidationResult.Failure("Failed to add dummy to the world"))
        DummyPlayerEvent.connect(holder, dummyRef, world)
        return result
    }

    // ===========================================================================================
    // UPDATING (Visuals)
    // ===========================================================================================

    /**
     * Updates the skin and model of an existing dummy.
     * @return true if successful, false if dummy not found or invalid
     */
    fun updateSkin(dummyRef: PlayerRef, newSkin: PlayerSkin): Boolean {
        val entity = dummyRef.reference ?: return false
        if (!entity.isValid) return false
        val store = entity.store

        val skinComponent = PlayerSkinComponent(newSkin)
        store.putComponent(entity, PlayerSkinComponent.getComponentType(), skinComponent)

        val newModel = CosmeticsModule.get().createModel(newSkin)
        if (newModel != null) {
            val modelComponent = ModelComponent(newModel)
            store.putComponent(entity, ModelComponent.getComponentType(), modelComponent)
        }
        return true
    }

    // ===========================================================================================
    // UTILITIES
    // ===========================================================================================

    fun deleteDummy(username: String): Boolean {
        val uuid = getDummyUUID(username)
        val dummyRef = dummies.remove(uuid) ?: return false
        val entity = dummyRef.reference
        if (entity != null && entity.isValid) {
            val world = entity.store.externalData.world
            tabListRemovePlayer(world, uuid)
        }
        try {
            dummyRef.packetHandler.channel.close()
        } catch (_: Exception) {
            // Ignore if already closed
        }
        Universe.get().removePlayer(dummyRef)
        return true
    }

    fun getDummy(name: String): PlayerRef? = dummies[getDummyUUID(name)]

    fun getDummy(uuid: UUID): PlayerRef? = dummies[uuid]

    fun getDummyUUID(username: String): UUID {
        return UUID.nameUUIDFromBytes("Dummy:$username".toByteArray(Charsets.UTF_8))
    }

    fun getDummyNames(): List<String> {
        return dummies.values.map { it.username }
    }

    private fun setupHolderComponents(
        holder: Holder<EntityStore>,
        dummyRef: PlayerRef,
        chunkTracker: ChunkTracker,
        uuid: UUID,
        skin: PlayerSkin?,
        dummyHandler: DummyPacketHandler,
        username: String
    ) {
        // Core Identity
        holder.addComponent(PlayerRef.getComponentType(), dummyRef)
        holder.addComponent(ChunkTracker.getComponentType(), chunkTracker)
        holder.addComponent(UUIDComponent.getComponentType(), UUIDComponent(uuid))
        holder.addComponent(Nameplate.getComponentType(), Nameplate(username))
        holder.addComponent(DisplayNameComponent.getComponentType(), DisplayNameComponent(Message.raw(username)))

        // Custom Component
        holder.addComponent(DummyPlayerPlugin.DUMMY_COMPONENT_TYPE, DummyComponent(dummyRef.uuid))

        // Physics & Position
        holder.addComponent(PositionDataComponent.getComponentType(), PositionDataComponent())
        holder.addComponent(PhysicsValues.getComponentType(), PhysicsValues())
        holder.addComponent(Velocity.getComponentType(), Velocity())
        holder.addComponent(BoundingBox.getComponentType(), BoundingBox())
        holder.addComponent(EntityScaleComponent.getComponentType(), EntityScaleComponent(1.0f))
        holder.addComponent(CollisionResultComponent.getComponentType(), CollisionResultComponent())

        // Movement & Animation
        holder.addComponent(MovementAudioComponent.getComponentType(), MovementAudioComponent())
        holder.addComponent(MovementStatesComponent.getComponentType(), createIdleMovementStates())
        holder.addComponent(HeadRotation.getComponentType(), HeadRotation())
        holder.addComponent(ActiveAnimationComponent.getComponentType(), ActiveAnimationComponent())

        // Game Logic
        val player = Player()
        holder.addComponent(Player.getComponentType(), player)
        player.init(uuid, dummyRef)
        player.clientViewRadius = 2

        holder.addComponent(DamageDataComponent.getComponentType(), DamageDataComponent())
        holder.addComponent(Interactable.getComponentType(), Interactable.INSTANCE)
        holder.addComponent(AudioComponent.getComponentType(), AudioComponent())

        // Knockback
        val knockbackComponent = KnockbackComponent()
        knockbackComponent.velocity = Vector3d(0.0, 0.0, 0.0)
        holder.addComponent(KnockbackComponent.getComponentType(), knockbackComponent)

        // Visuals (Skin & Model)
        val actualSkin = skin ?: createDefaultSkin()
        holder.addComponent(PlayerSkinComponent.getComponentType(), PlayerSkinComponent(actualSkin))
        holder.addComponent(
            ModelComponent.getComponentType(),
            ModelComponent(CosmeticsModule.get().createModel(actualSkin))
        )

        // Tracker
        dummyHandler.setPlayerRef(dummyRef, player)
        val entityViewer = EntityTrackerSystems.EntityViewer(2 * 32, dummyHandler)
        holder.addComponent(EntityTrackerSystems.EntityViewer.getComponentType(), entityViewer)
    }

    /**
     * Reflectively add dummy to Universe. players map.
     */
    private fun modifyUniversePlayersMap(action: (MutableMap<UUID, PlayerRef>) -> Unit) {
        try {
            val universe = Universe.get()
            val playersField = Universe::class.java.getDeclaredField("players")
            playersField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val playersMap = playersField.get(universe) as MutableMap<UUID, PlayerRef>
            action(playersMap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sends packet to update Tab List
    private fun tabListAddPlayer(world: World, uuid: UUID, name: String) {
        val listPlayer = ServerPlayerListPlayer(uuid, name, world.worldConfig.uuid, 0)
        val addPacket = AddToServerPlayerList(arrayOf(listPlayer))
        world.playerRefs.forEach { p ->
            p.packetHandler.writeNoCache(addPacket)
        }
    }

    private fun tabListRemovePlayer(world: World, uuid: UUID) {
        val removePacket = RemoveFromServerPlayerList(arrayOf(uuid))
        world.playerRefs.forEach { p ->
            if (p.packetHandler !is DummyPacketHandler) {
                p.packetHandler.writeNoCache(removePacket)
            }
        }
    }

    private fun createIdleMovementStates(): MovementStatesComponent {
        val movementStates = MovementStatesComponent()
        movementStates.movementStates.idle = true
        return movementStates
    }

    private fun createDefaultSkin(): PlayerSkin {
        // A safe default Steve/Alex look
        return PlayerSkin().apply {
            bodyCharacteristic = "human_male"
            underwear = "underwear_male"
            face = "face_a"
            eyes = "eyes_male"
            ears = "ears_a"
            mouth = "mouth_a"
            haircut = "hair_short_messy"
            eyebrows = "eyebrows_thick"
            pants = "pants_shorts_denim"
            undertop = "shirt_tshirt"
            shoes = "shoes_sneakers"
        }
    }
}