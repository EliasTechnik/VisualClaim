package dev.ewio

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.scope
import dev.ewio.claim.repository.ChunkRepository
import dev.ewio.claim.repository.ClaimRepository
import dev.ewio.claim.repository.PlayerRepository
import dev.ewio.claim.service.ClaimService
import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCPlayer
import dev.ewio.claim.definitions.VCRestrictions
import dev.ewio.claim.service.CentralCache
import dev.ewio.claim.service.MessageService
import dev.ewio.claim.service.MovementService
import dev.ewio.claim.service.PermissionService
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.command.AutoclaimCommand
import dev.ewio.command.ClaimCommand
import dev.ewio.command.ClaiminfoCommand
import dev.ewio.command.DeleteclaimCommand
import dev.ewio.command.ListclaimsCommand
import dev.ewio.command.RenameclaimCommand
import dev.ewio.command.UnclaimCommand
import dev.ewio.database.VCDB
import dev.ewio.listener.JoinListener
import dev.ewio.listener.LeaveListener
import dev.ewio.map.MapService
import dev.ewio.map.NoopMapService
import dev.ewio.map.Pl3xMapService
import dev.ewio.util.GL
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File


class VisualClaim : JavaPlugin() {
    lateinit var mapService: MapService
    lateinit var claimService: ClaimService
    lateinit var permissionService: PermissionService
    lateinit var prerequisiteService: PrerequisiteService
    lateinit var movementService: MovementService
    lateinit var centralCache: CentralCache
    lateinit var cfg: FileConfiguration
    lateinit var messageService: MessageService

    lateinit var joinListner: JoinListener
    lateinit var leaveListener: LeaveListener

    override fun onEnable() {
        // Plugin startup logic
        GL.init(this)

        saveDefaultConfig()
        cfg = config

        val defaultRestrictions = VCRestrictions(
            maxClaims = cfg.getInt("limits.max-claims-per-player", 10),
            maxChunks = cfg.getInt("limits.max-chunks-per-player", 100),
            maxClaimNameLength = if(cfg.getInt("limits.max-claim-name-length", 32) <= 250) {
                cfg.getInt("limits.max-claim-name-length", 32)
            } else {
                250 //database limit
            }
        )

        //init database
        VCDB.connect(File(dataFolder, "VisualClaim.db").absolutePath)


        //services
        this.messageService = MessageService(cfg)
        messageService.load()

        this.claimService = ClaimService(
            claimRepo = ClaimRepository(),//InMemoryRepository<VCClaim>(extractKey = { it.key }),
            playerRepo = PlayerRepository(),//InMemoryRepository<VCPlayer>(extractKey = { it.key }),
            chunkRepo = ChunkRepository(), //InMemoryRepository<VCChunk>(extractKey = { it.key }),
            placeOnMap = { player, claim, chunks -> placeOnMap(player, claim, chunks) },
            deleteFromMap = { deletedClaim -> deleteFromMap(deletedClaim) }
        )

        val triggerWordsFromConfig = mutableListOf<String>()
        cfg.getStringList("trigger-words")?.let{
            triggerWordsFromConfig.addAll(it)
        }
        cfg.getString("migration-name")?.let{
            triggerWordsFromConfig.add(it)
        }


        this.permissionService = PermissionService(
            defaultVCRestrictions = defaultRestrictions,
            triggerWords = triggerWordsFromConfig
        )
        this.centralCache = CentralCache(
            coroutineScope = this.scope,
            claimService = this.claimService,
            permissionService = this.permissionService
        )
        this.prerequisiteService = PrerequisiteService(
            claimService = this.claimService,
            permissionService = this.permissionService,
            coroutineScope = this.scope,
            cc = centralCache
        )
        this.mapService = if(isPl3xMapPresent()) {
            Pl3xMapService(this)
        } else {
            NoopMapService()
        }

        //migrate claims if they contain trigger words as names
        launch{
            val count = claimService.removeTriggerWordsFromClaims(
                permissionService.triggerWords,
                cfg.getString("migration-name")?: "_renamed"
            )
            if(count > 0){
                GL.logger.info("Renamed $count claims with conflicting names. This happens when trigger-words are changed.")
            }
        }


        if(mapService.isActive()){
            launch {
                claimService.deleteAllClaimsFromMap()
                claimService.placeAllClaimsOnMap()
            }
        }

        this.movementService = MovementService(
            registerListener = { listener ->
                server.pluginManager.registerEvents(listener, this)
            },
            preService = this.prerequisiteService,
            coroutineScope = this.scope,
            cc = centralCache,
            getStringFromConfig = { path -> getStringFormConfig(path) },
            ms = messageService
        )

        //Listeners (which aren't part of services) can be registered here
        this.joinListner = JoinListener()
        server.pluginManager.registerEvents(this.joinListner, this)
        this.leaveListener = LeaveListener()
        server.pluginManager.registerEvents(this.leaveListener, this)


        // Commands
        getCommand("claim")?.setExecutor(
            ClaimCommand(
                preService = prerequisiteService,
                coroutineScope = this.scope,
                getStringFromConfig = { path -> getStringFormConfig(path) },
                ms = messageService
            )
        )

        getCommand("listclaims")?.setExecutor(
            ListclaimsCommand(
                preService = prerequisiteService,
                coroutineScope = this.scope,
                getStringFromConfig = { path -> getStringFormConfig(path) },
                ms = messageService
            )
        )

        getCommand("claiminfo")?.setExecutor(
                ClaiminfoCommand(
                preService = prerequisiteService,
                coroutineScope = this.scope,
                getStringFromConfig = { path -> getStringFormConfig(path) },
                    ms = messageService
            )
        )

        getCommand("unclaim")?.setExecutor(
            UnclaimCommand(
                preService = prerequisiteService,
                coroutineScope = this.scope,
                getStringFromConfig = { path -> getStringFormConfig(path) },
                ms = messageService
            )
        )

        getCommand("deleteclaim")?.setExecutor(
            DeleteclaimCommand(
                preService = prerequisiteService,
                coroutineScope = this.scope,
                getStringFromConfig = { path -> getStringFormConfig(path) },
                ms = messageService
            )
        )
        getCommand("renameclaim")?.setExecutor(
            RenameclaimCommand(
                preService = prerequisiteService,
                coroutineScope = this.scope,
                getStringFromConfig = { path -> getStringFormConfig(path) },
                ms = messageService
            )
        )
        getCommand("autoclaim")?.setExecutor(
            AutoclaimCommand(
                preService = prerequisiteService,
                coroutineScope = this.scope,
                getStringFromConfig = { path -> getStringFormConfig(path) },
                ms = messageService
            )
        )

        logger.info("VisualClaim activated. Pl3xMap: " + (if (mapService.isActive()) "active" else "not found"))
        logger.info("VisualClaim activated.")
    }

    override fun onDisable() {
        // Plugin shutdown logic
        try {
            mapService.shutdown()
        } catch (e: Exception) {
            GL.logger.severe("Error shutting down map service: ${e.message}")
        }
        VCDB.shutdown()
    }

    fun isPl3xMapPresent(): Boolean {
        return Bukkit.getPluginManager().getPlugin("Pl3xMap") != null
    }

    private fun placeOnMap(player: VCPlayer, claim: VCClaim, chunks: List<VCChunk>) {
        mapService.writeClaimMarker(player, claim, chunks)
    }

    private fun deleteFromMap(chunksToDeleted: List<VCChunk>) {
        mapService.removeChunkMarker(chunksToDeleted)
    }

    private fun getStringFormConfig(path: String): String {
        return cfg.getString(path) ?: path
    }
}