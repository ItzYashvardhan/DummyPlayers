package me.justlime.dummyplayer.service

import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
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
import me.justlime.dummyplayer.packets.DummyPacketHandler
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object DummyPlayerFactory {

    private val cloneCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val dummies = ConcurrentHashMap<String, Ref<EntityStore>>()

    fun spawnDummy(
        world: World,
        username: String,
        position: Vector3d,
        skin: PlayerSkin? = null
    ): CompletableFuture<Ref<EntityStore>?> {
        val existingRef = dummies[username]
        if (existingRef != null) {
            if (existingRef.isValid) {
                return CompletableFuture.completedFuture(null)
            }
            dummies.remove(username)
        }

        val uuid = UUID.randomUUID()
        val holder = EntityStore.REGISTRY.newHolder()
        val embeddedChannel = EmbeddedChannel()
        val protocolVersion = ProtocolVersion(ProtocolSettings.PROTOCOL_VERSION)
        val authentication =  PlayerAuthentication(uuid, username)
        val dummyHandler = DummyPacketHandler(embeddedChannel, protocolVersion, authentication)
        val chunkTracker = ChunkTracker()

        val playerRef = PlayerRef(holder, uuid, username, "en_US", dummyHandler, chunkTracker)

        setupHolderComponents(holder, playerRef, chunkTracker, uuid, skin, dummyHandler, username)

        modifyUniversePlayersMap { it[uuid] = playerRef }

        val spawnPos = Vector3d(position.x, position.y + 1.0, position.z)
        val initialTransform = TransformComponent(spawnPos, Vector3f(0f, 0f, 0f))
        holder.addComponent(TransformComponent.getComponentType(), initialTransform)
        val spawnTransform = Transform(spawnPos)

        return world.addPlayer(playerRef, spawnTransform)?.thenApply { ref ->
            if (ref != null) {
                val entityRef = ref.reference
                if (entityRef != null) {
                    dummies[username] = entityRef
                    broadcastAddPlayer(world, uuid, username)
                    return@thenApply entityRef
                }
            }
            return@thenApply null
        } ?: CompletableFuture.completedFuture(null)
    }

    fun deleteDummy(world: World, name: String): Boolean {
        val ref = dummies.remove(name)
        if (ref != null && ref.isValid) {
            val playerRef = world.entityStore.store.getComponent(ref, PlayerRef.getComponentType())

            if (playerRef != null) {
                broadcastRemovePlayer(world, playerRef.uuid)
                modifyUniversePlayersMap { it.remove(playerRef.uuid) }
                playerRef.removeFromStore()
            } else {
                world.entityStore.store.removeEntity(ref, RemoveReason.REMOVE)
            }

            return true
        }
        return false
    }

    fun getDummy(name: String): Ref<EntityStore>? {
        return dummies[name]
    }

    fun getDummyNames(): List<String> {
        return dummies.keys.toList()
    }

    fun cloneDummy(
        world: World,
        originalPlayerRef: Ref<EntityStore>,
        requesterSkin: PlayerSkin? = null
    ): CompletableFuture<Ref<EntityStore>?> {
        val store = world.entityStore.store

        if (!originalPlayerRef.isValid) return CompletableFuture.completedFuture(null)

        val originalPlayerComponent = store.getComponent(originalPlayerRef, PlayerRef.getComponentType())
        val originalName = originalPlayerComponent?.username ?: "Unknown"

        val count = cloneCounts.computeIfAbsent(originalName) { AtomicInteger(0) }.incrementAndGet()
        val newName = "${originalName}Clone$count"

        val skin = requesterSkin ?: store.getComponent(originalPlayerRef, PlayerSkinComponent.getComponentType())?.playerSkin ?: createDefaultSkin()

        val originalTransform = store.getComponent(originalPlayerRef, TransformComponent.getComponentType())
        val originalPos = originalTransform?.position ?: Vector3d(0.0, 0.0, 0.0)
        val newPos = Vector3d(originalPos.x, originalPos.y + 1.0, originalPos.z)

        return spawnDummy(world, newName, newPos, skin)
    }

    private fun setupHolderComponents(
        holder: Holder<EntityStore>,
        playerRef: PlayerRef,
        chunkTracker: ChunkTracker,
        uuid: UUID,
        skin: PlayerSkin?,
        dummyHandler: DummyPacketHandler,
        name: String
    ) {
        holder.addComponent(PlayerRef.getComponentType(), playerRef)
        holder.addComponent(ChunkTracker.getComponentType(), chunkTracker)
        holder.addComponent(UUIDComponent.getComponentType(), UUIDComponent(uuid))
        holder.addComponent(PositionDataComponent.getComponentType(), PositionDataComponent())
        holder.addComponent(MovementAudioComponent.getComponentType(), MovementAudioComponent())
        holder.addComponent(MovementStatesComponent.getComponentType(), createIdleMovementStates())

        val player = Player()
        holder.addComponent(Player.getComponentType(), player)
        player.init(uuid, playerRef)

        val actualSkin = skin ?: createDefaultSkin()
        holder.addComponent(PlayerSkinComponent.getComponentType(), PlayerSkinComponent(actualSkin))
        holder.addComponent(
            ModelComponent.getComponentType(),
            ModelComponent(CosmeticsModule.get().createModel(actualSkin))
        )

        dummyHandler.setPlayerRef(playerRef, player)

        player.clientViewRadius = 2
        val entityViewer = EntityTrackerSystems.EntityViewer(2 * 32, dummyHandler)
        holder.addComponent(EntityTrackerSystems.EntityViewer.getComponentType(), entityViewer)

        holder.addComponent(PhysicsValues.getComponentType(), PhysicsValues())
        holder.addComponent(Velocity.getComponentType(), Velocity())
        holder.addComponent(DamageDataComponent.getComponentType(), DamageDataComponent())
        
        // Additional components from com.hypixel.hytale.server.core.entity
        val knockbackComponent = KnockbackComponent()
        knockbackComponent.velocity = Vector3d(0.0, 0.0, 0.0)
        holder.addComponent(KnockbackComponent.getComponentType(), knockbackComponent)
        holder.addComponent(Nameplate.getComponentType(), Nameplate(name))

        // Additional components from com.hypixel.hytale.server.core.modules.entity.component
        holder.addComponent(HeadRotation.getComponentType(), HeadRotation())
        holder.addComponent(Interactable.getComponentType(), Interactable.INSTANCE)
        holder.addComponent(AudioComponent.getComponentType(), AudioComponent())
        holder.addComponent(DisplayNameComponent.getComponentType(), DisplayNameComponent(Message.raw(name)))
        holder.addComponent(EntityScaleComponent.getComponentType(), EntityScaleComponent(1.0f))
        holder.addComponent(ActiveAnimationComponent.getComponentType(), ActiveAnimationComponent())
        holder.addComponent(CollisionResultComponent.getComponentType(), CollisionResultComponent())
        holder.addComponent(BoundingBox.getComponentType(), BoundingBox())
    }

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

    private fun broadcastAddPlayer(world: World, uuid: UUID, name: String) {
        val listPlayer = ServerPlayerListPlayer(
            uuid,
            name,
            world.worldConfig.uuid,
            0
        )
        val addPacket = AddToServerPlayerList(arrayOf(listPlayer))
        world.playerRefs.forEach { p ->
            p.packetHandler.writeNoCache(addPacket)
        }
    }

    private fun broadcastRemovePlayer(world: World, uuid: UUID) {
        val removePacket = RemoveFromServerPlayerList(arrayOf(uuid))
        world.playerRefs.forEach { p ->
            p.packetHandler.writeNoCache(removePacket)
        }
    }

    private fun createIdleMovementStates(): MovementStatesComponent {
        val movementStates = MovementStatesComponent()
        movementStates.movementStates.idle = true
        movementStates.movementStates.walking = false
        return movementStates
    }

    private fun createDefaultSkin(): PlayerSkin {
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
