package dev.ewio

import dev.ewio.claim.ClaimService
import dev.ewio.claim.repository.definitions.VCChunk
import dev.ewio.claim.repository.definitions.VCClaim
import dev.ewio.claim.repository.definitions.VCPlayer
import dev.ewio.claim.repository.adapter.InMemoryRepository
import dev.ewio.claim.repository.adapter.exposed.ExposedChunkRepository
import dev.ewio.claim.repository.adapter.exposed.ExposedClaimRepository
import dev.ewio.claim.repository.adapter.exposed.ExposedPlayerRepository
import dev.ewio.command.ClaimCommand
import dev.ewio.command.ClaiminfoCommand
import dev.ewio.command.DeleteclaimCommand
import dev.ewio.command.ListclaimsCommand
import dev.ewio.command.RenameclaimCommand
import dev.ewio.command.UnclaimCommand
import dev.ewio.database.VCDB
import dev.ewio.map.MapService
import dev.ewio.map.NoopMapService
import dev.ewio.map.Pl3xMapService
import dev.ewio.permission.PermissionService
import dev.ewio.util.StringHelper
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File


class VisualClaim : JavaPlugin() {
    lateinit var mapService: MapService
    lateinit var claimService: ClaimService
    lateinit var permissionService: PermissionService
    lateinit var cfg: FileConfiguration
    lateinit var strings: StringHelper

    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        cfg = config

        //init database
        VCDB.init(File(dataFolder, "VisualClaim.db"))

        //services
        this.claimService = ClaimService(
            claimRepository = ExposedClaimRepository(),//InMemoryRepository<VCClaim>(extractKey = { it.key }),
            playerRepository = ExposedPlayerRepository(),//InMemoryRepository<VCPlayer>(extractKey = { it.key }),
            chunkRepository = ExposedChunkRepository(), //InMemoryRepository<VCChunk>(extractKey = { it.key }),
            plugin = this,
            partialMapUpdate = { changedClaim -> partialMapUpdate(changedClaim) },
            deleteFromMap = { deletedClaim -> deleteFromMap(deletedClaim) }
        )
        this.permissionService = PermissionService(this)
        this.mapService = if(isPl3xMapPresent()) {
            Pl3xMapService(this)
        } else {
            NoopMapService()
        }
        this.strings = StringHelper(this)

        //TODO
        // 4) (Optional) Bestehende Claims in die Karte pushen – NICHT im Main-Thread
        /*
        server.scheduler.runTaskAsynchronously(this) {
            val chunks = chunkRepo.all()
            // Falls du initial Marker zeichnen willst:
            // gruppiere nach claimKey und rufe partialMapUpdate(...) für jeden Claim auf
        }*/


        // Commands
        getCommand("claim")?.setExecutor(ClaimCommand(this))
        getCommand("listclaims")?.setExecutor(ListclaimsCommand(this))
        getCommand("claiminfo")?.setExecutor(ClaiminfoCommand(this))
        getCommand("unclaim")?.setExecutor(UnclaimCommand(this))
        getCommand("deleteclaim")?.setExecutor(DeleteclaimCommand(this))
        getCommand("renameclaim")?.setExecutor(RenameclaimCommand(this))

        logger.info("VisualClaim activated. Pl3xMap: " + (if (mapService.isActive()) "active" else "not found"))
        logger.info("VisualClaim activated.")
    }

    override fun onDisable() {
        // Plugin shutdown logic
        mapService.shutdown()
        VCDB.shutdown()
        //TODO: Remove all markers from map?

    }

    fun isPl3xMapPresent(): Boolean {
        return Bukkit.getPluginManager().getPlugin("Pl3xMap") != null
    }

    private fun partialMapUpdate(changedClaim: VCClaim) {
        // if a claim changes we remove it completely and re-add it
        //WARNING: This only works if the claim is still in the database!
        mapService.removeClaimMarker(changedClaim)
        mapService.writeClaimMarker(changedClaim)
    }

    private fun deleteFromMap(chunksToDeleted: List<VCChunk>) {
        mapService.removeChunkMarker(chunksToDeleted)
    }
}