package dev.ewio

import dev.ewio.claim.ClaimService
import dev.ewio.claim.VCChunk
import dev.ewio.claim.VCClaim
import dev.ewio.claim.VCPlayer
import dev.ewio.claim.repository.adapter.InMemoryRepository
import dev.ewio.command.ClaimCommand
import dev.ewio.command.ClaiminfoCommand
import dev.ewio.command.DeleteclaimCommand
import dev.ewio.command.ListclaimsCommand
import dev.ewio.command.UnclaimCommand
import dev.ewio.map.MapService
import dev.ewio.map.NoopMapService
import dev.ewio.map.Pl3xMapService
import dev.ewio.permission.PermissionService
import dev.ewio.util.StringHelper
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin


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

        //services
        this.claimService = ClaimService(
            claimRepository = InMemoryRepository<VCClaim>(extractKey = { it.key }, isDeleted = {it.deleted}),
            playerRepository = InMemoryRepository<VCPlayer>(extractKey = { it.key }, isDeleted = {false}), //not the cleanest of all but player should never be deleted
            chunkRepository = InMemoryRepository<VCChunk>(extractKey = { it.key }, isDeleted = {it.deleted}),
            plugin = this,
            partialMapUpdate = { changedClaim -> partialMapUpdate(changedClaim) },
            deleteFromMap = { deletedClaim -> deleteFromMap(deletedClaim) }
        )

        //purge previous deleted claims
        claimService.purgeDB()

        this.permissionService = PermissionService(this)
        this.mapService = if(isPl3xMapPresent()) {
            Pl3xMapService(this)
        } else {
            NoopMapService()
        }
        this.strings = StringHelper(this)

        // Commands
        getCommand("claim")?.setExecutor(ClaimCommand(this))
        getCommand("listclaims")?.setExecutor(ListclaimsCommand(this))
        getCommand("claiminfo")?.setExecutor(ClaiminfoCommand(this))
        getCommand("unclaim")?.setExecutor(UnclaimCommand(this))
        getCommand("deleteclaim")?.setExecutor(DeleteclaimCommand(this))

        logger.info("VisualClaim activated. Pl3xMap: " + (if (mapService.isActive()) "active" else "not found"))
        logger.info("VisualClaim activated.")
    }

    override fun onDisable() {
        // Plugin shutdown logic
        mapService.shutdown();
    }

    fun isPl3xMapPresent(): Boolean {
        return Bukkit.getPluginManager().getPlugin("Pl3xMap") != null
    }

    private fun partialMapUpdate(changedClaim: VCClaim) {
        // if a claim changes we remove it completely and re-add it
        mapService.removeClaimMarker(changedClaim)
        mapService.writeClaimMarker(changedClaim)
    }

    private fun deleteFromMap(deletedClaim: VCClaim) {
        mapService.removeClaimMarker(deletedClaim)
    }
}