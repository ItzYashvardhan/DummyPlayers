package me.justlime.dummyplayer.service

import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.protocol.NetworkChannel
import com.hypixel.hytale.protocol.PlayerSkin
import com.hypixel.hytale.protocol.ProtocolSettings
import com.hypixel.hytale.protocol.packets.interface_.AddToServerPlayerList
import com.hypixel.hytale.protocol.packets.interface_.RemoveFromServerPlayerList
import com.hypixel.hytale.protocol.packets.interface_.ServerPlayerListPlayer
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.auth.PlayerAuthentication
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
import com.hypixel.hytale.server.core.entity.InteractionManager
import com.hypixel.hytale.server.core.entity.UUIDComponent
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.io.ProtocolVersion
import com.hypixel.hytale.server.core.modules.entity.component.*
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.server.npc.asset.builder.Builder
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.server.npc.interactions.NPCInteractionSimulationHandler
import com.hypixel.hytale.server.npc.role.Role
import com.hypixel.hytale.server.npc.role.support.RoleStats
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem
import com.hypixel.hytale.server.npc.util.expression.ExecutionContext
import io.netty.channel.embedded.EmbeddedChannel
import me.justlime.dummyplayer.component.DummyPlayerComponent
import me.justlime.dummyplayer.listener.DummyPlayerEvent
import me.justlime.dummyplayer.model.enums.DummyValidationResult
import me.justlime.dummyplayer.network.DummyPacketHandler
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object DummyPlayerService {

    //    private val cloneCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val dummies = ConcurrentHashMap<UUID, PlayerRef>()
    private val holders = ConcurrentHashMap<UUID, Holder<EntityStore>>()

    // ===========================================================================================
    // SPAWNING
    // ===========================================================================================

    fun spawnDummy(
        world: World,
        username: String,
        position: Vector3d,
        rotation: Vector3f,
        skin: PlayerSkin
    ): CompletableFuture<DummyValidationResult> {
        val uuid = getDummyUUID(username)

        val existingRef = dummies[uuid]
        if (existingRef != null) {
            if (existingRef.isValid) {
                return CompletableFuture.completedFuture(DummyValidationResult.AlreadyExists(existingRef))
            }
            dummies.remove(uuid)
            holders.remove(uuid)
        }

        // Core Network Objects
        val holder = EntityStore.REGISTRY.newHolder()
        val embeddedChannel = EmbeddedChannel()
        val protocolVersion = ProtocolVersion(ProtocolSettings.PROTOCOL_VERSION)
        val authentication = PlayerAuthentication(uuid, username)
        val dummyHandler = DummyPacketHandler(embeddedChannel, protocolVersion, authentication)
        for (nc in NetworkChannel.VALUES) {
            dummyHandler.setChannel(nc, embeddedChannel)
        }
        val event = DummyPlayerEvent.setupConnect(dummyHandler, username, uuid, authentication)
        if (event != null && event.isCancelled) {
            val reason = event.reason ?: "Connection rejected."
            return CompletableFuture.completedFuture(DummyValidationResult.ConnectionDenied(reason))
        }

        // Setup Entity
        val chunkTracker = ChunkTracker()
        val spawnRot = Vector3f(rotation.x, rotation.y, rotation.z)
        val spawnPos = Vector3d(position.x, position.y + 1.0, position.z)
        val spawnTransform = Transform(spawnPos, spawnRot)
        val dummyRef = PlayerRef(holder, uuid, username, "en_US", dummyHandler, chunkTracker)
        setupHolderComponents(holder, dummyRef, chunkTracker, uuid, skin, dummyHandler, username, spawnPos, spawnRot, world)

        // Add to World
        modifyUniversePlayersMap { it[uuid] = dummyRef }
        val result: CompletableFuture<DummyValidationResult> =
            world.addPlayer(dummyRef, spawnTransform)?.thenApply { ref ->
                if (ref != null && ref.isValid) {
                    dummies[uuid] = dummyRef
                    holders[uuid] = holder
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
        holders.remove(uuid)
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

    fun setRole(dummyName: String, roleName: String) {
        val ref = getDummy(dummyName)?.reference ?: return
        val store = ref.store
        val npc = store.getComponent(ref, NPCEntity.getComponentType()!!) ?: return
        val roleIndex = NPCPlugin.get().getIndex(roleName)
        RoleChangeSystem.requestRoleChange(ref, npc.role!!, roleIndex, true, store)
    }

    fun getHighestDummySuffixAndName(baseName: String): Pair<Int, String> {
        val allNames = getDummyNames()
        var maxSuffix = 0
        var lastDummyName = baseName

        for (existingName in allNames) {
            if (existingName == baseName) {
                if (maxSuffix == 0) maxSuffix = 1
            } else if (existingName.startsWith("${baseName}_")) {
                val suffixStr = existingName.substringAfter("${baseName}_")
                val suffixInt = suffixStr.toIntOrNull()
                if (suffixInt != null && suffixInt > maxSuffix) {
                    maxSuffix = suffixInt
                    lastDummyName = existingName
                }
            }
        }
        return Pair(maxSuffix, lastDummyName)
    }

    internal fun createNPCEntity(
        holder: Holder<EntityStore>,
        position: Vector3d? = null,
        rotation: Vector3f? = null,
        world: World? = null
    ): NPCEntity {
        val npcEntity = NPCEntity()
        val roleName = "Dummy_Player"
        npcEntity.roleName = roleName
        npcEntity.roleIndex = NPCPlugin.get().getIndex(roleName)

        if (world != null) {
            val worldTime = world.entityStore.store.getResource(WorldTimeResource.getResourceType())
            npcEntity.spawnInstant = worldTime.gameTime
        }

        if (position != null && rotation != null) {
            npcEntity.saveLeashInformation(position, rotation)
        }

        val plugin = NPCPlugin.get()
        val roleIndex = npcEntity.roleIndex
        val builderInfo = plugin.prepareRoleBuilderInfo(roleIndex)

        @Suppress("UNCHECKED_CAST")
        val roleBuilder = builderInfo.builder as Builder<Role>

        val executionContext = ExecutionContext()
        val roleStats = RoleStats()

        val builderSupport = BuilderSupport(
            plugin.builderManager,
            npcEntity,
            holder,
            executionContext,
            roleBuilder,
            roleStats
        )

        val role = NPCPlugin.buildRole(roleBuilder, builderInfo, builderSupport, roleIndex)
        npcEntity.role = role
        role.spawned(holder, npcEntity)
        return npcEntity
    }

    private fun setupHolderComponents(
        holder: Holder<EntityStore>,
        dummyRef: PlayerRef,
        chunkTracker: ChunkTracker,
        uuid: UUID,
        skin: PlayerSkin,
        dummyHandler: DummyPacketHandler,
        username: String,
        spawnPos: Vector3d,
        spawnRot: Vector3f,
        world: World
    ) {
        // Core Reference & Tracking
        chunkTracker.setDefaultMaxChunksPerSecond(dummyRef)
        holder.putComponent(PlayerRef.getComponentType(), dummyRef)
        holder.putComponent(ChunkTracker.getComponentType(), chunkTracker)
        holder.putComponent(UUIDComponent.getComponentType(), UUIDComponent(uuid))

        //  Position & Movement
        holder.addComponent(TransformComponent.getComponentType(), TransformComponent(spawnPos, spawnRot))
        holder.ensureComponent(PositionDataComponent.getComponentType())
        holder.ensureComponent(MovementAudioComponent.getComponentType())

        // Player Identity
        val playerComponent = holder.ensureAndGetComponent(Player.getComponentType())
        playerComponent.init(uuid, dummyRef)
        playerComponent.playerConfigData.cleanup(Universe.get()) // Clean up config like the server does

        // Set View Radius
        val viewRadiusChunks = 4
        playerComponent.clientViewRadius = viewRadiusChunks * 32

        // Visuals (Skin, Model, Name)
        holder.putComponent(PlayerSkinComponent.getComponentType(), PlayerSkinComponent(skin))
        holder.putComponent(ModelComponent.getComponentType(), ModelComponent(CosmeticsModule.get().createModel(skin)))
        holder.putComponent(Nameplate.getComponentType(), Nameplate(username))
        holder.putComponent(DisplayNameComponent.getComponentType(), DisplayNameComponent(Message.raw(username)))

        // NPC Specifics
        holder.addComponent(DummyPlayerComponent.getComponentType(), DummyPlayerComponent())
        val npcEntity = createNPCEntity(holder, spawnPos, spawnRot, world)
        holder.addComponent(NPCEntity.getComponentType()!!, npcEntity)
        holder.addComponent(
            InteractionModule.get().interactionManagerComponent,
            InteractionManager(npcEntity, null, NPCInteractionSimulationHandler())
        )

        // Network / Viewer
        dummyHandler.setPlayerRef(dummyRef, playerComponent)
        val entityViewer = EntityTrackerSystems.EntityViewer(playerComponent.viewRadius * 32, dummyHandler)
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

}