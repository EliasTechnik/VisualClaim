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
import dev.ewio.claim.service.PermissionService
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.command.ClaimCommand

import dev.ewio.database.VCDB
import dev.ewio.map.MapService
import dev.ewio.map.NoopMapService
import dev.ewio.map.Pl3xMapService
import dev.ewio.util.GL
import dev.ewio.util.StringHelper
import dev.ewio.util.log
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File


class VisualClaim : JavaPlugin() {
    lateinit var mapService: MapService
    lateinit var claimService: ClaimService
    lateinit var permissionService: PermissionService
    lateinit var prerequisiteService: PrerequisiteService
    lateinit var cfg: FileConfiguration
    lateinit var strings: StringHelper

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
        this.claimService = ClaimService(
            claimRepo = ClaimRepository(),//InMemoryRepository<VCClaim>(extractKey = { it.key }),
            playerRepo = PlayerRepository(),//InMemoryRepository<VCPlayer>(extractKey = { it.key }),
            chunkRepo = ChunkRepository(), //InMemoryRepository<VCChunk>(extractKey = { it.key }),
            placeOnMap = { player, claim, chunks -> placeOnMap(player, claim, chunks) },
            deleteFromMap = { deletedClaim -> deleteFromMap(deletedClaim) }
        )
        this.permissionService = PermissionService(defaultVCRestrictions = defaultRestrictions)
        this.prerequisiteService = PrerequisiteService(
            claimService = this.claimService,
            permissionService = this.permissionService,
            coroutineScope = this.scope
        )
        this.mapService = if(isPl3xMapPresent()) {
            Pl3xMapService(this)
        } else {
            NoopMapService()
        }
        this.strings = StringHelper(this)

        //TODO
        // 4) (Optional) Bestehende Claims in die Karte pushen – NICHT im Main-Thread

        if(mapService.isActive()){
            launch {
                claimService.deleteAllClaimsFromMap()
                claimService.placeAllClaimsOnMap()
            }
        }

        /*
        server.scheduler.runTaskAsynchronously(this) {
            val chunks = chunkRepo.all()
            // Falls du initial Marker zeichnen willst:
            // gruppiere nach claimKey und rufe partialMapUpdate(...) für jeden Claim auf
        }*/


        // Commands
        getCommand("claim")?.setExecutor(ClaimCommand(
            preService = prerequisiteService,
            coroutineScope = this.scope,
            getStringFromConfig = { path -> getStringFormConfig(path) }
        ))
        /*
        getCommand("listclaims")?.setExecutor(ListclaimsCommand(this))

        getCommand("claiminfo")?.setExecutor(ClaiminfoCommand(this))
        getCommand("unclaim")?.setExecutor(UnclaimCommand(this))
        getCommand("deleteclaim")?.setExecutor(DeleteclaimCommand(this))
        getCommand("renameclaim")?.setExecutor(RenameclaimCommand(this))
*/
        logger.info("VisualClaim activated. Pl3xMap: " + (if (mapService.isActive()) "active" else "not found"))
        logger.info("VisualClaim activated.")
    }

    override fun onDisable() {
        // Plugin shutdown logic
        mapService.shutdown()
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