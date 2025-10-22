package dev.ewio.map

import dev.ewio.VisualClaim
import dev.ewio.claim.VCChunk
import dev.ewio.claim.VCClaim
import dev.ewio.claim.VCPlayer
import net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger

import net.pl3x.map.core.Pl3xMap
import net.pl3x.map.core.markers.layer.Layer
import net.pl3x.map.core.markers.layer.SimpleLayer
import net.pl3x.map.core.markers.marker.Rectangle
import net.pl3x.map.core.markers.option.Fill
import net.pl3x.map.core.markers.option.Options
import net.pl3x.map.core.markers.option.Stroke
import net.pl3x.map.core.registry.Registry
import org.bukkit.configuration.file.FileConfiguration
import java.util.*
import java.util.function.Supplier


class Pl3xMapService: MapService {
    private var plugin: VisualClaim
    private val layers: MutableMap<UUID?, SimpleLayer?> = HashMap<UUID?, SimpleLayer?>() // pro Welt ein Layer
    private var strokeColor = 0
    private var strokeWeight: Int = 0
    private var fillColor: Int = 0

    constructor(plugin: VisualClaim) {
        this.plugin = plugin
        val cfg: FileConfiguration = plugin.cfg
        this.strokeColor = cfg.getInt("marker.stroke-color", -0xff0100)
        this.strokeWeight = cfg.getInt("marker.stroke-weight", 2)
        this.fillColor = cfg.getInt("marker.fill-color", 0x6600FF00)

        // register layers per world
        val api = Pl3xMap.api()
        for (world in api.worldRegistry.values()) {
            val reg: Registry<Layer> = world.layerRegistry
            val layer = SimpleLayer("visualclaim", Supplier { cfg.get("Pl3xMap.layer-name")?.toString() ?: "Claims" })
            layer.setShowControls(true).setLiveUpdate(true).zIndex = 250
            reg.register(layer)
            layers[UUID.nameUUIDFromBytes(world.name.toByteArray())] = layer // Key based on worldname
        }
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun writeClaimMarker(claim: VCClaim) {
        //get all chunks of the claim
        val chunks = plugin.claimService.getChunksOfClaim(claim)
        val player = plugin.claimService.getPlayerByKey(claim.playerKey) ?: return

        plugin.logger.info("Writing markers for claim ${claim.displayName} owned by ${player.name} over ${chunks.size} chunks.")

        //add marker for each chunk
        chunks.forEach {
            writeChunkbasedClaimMarker(claim, player, it)
        }
    }

    private fun writeChunkbasedClaimMarker(claim: VCClaim, player: VCPlayer, chunk: VCChunk) {
        //get world
        val world = Pl3xMap.api().worldRegistry.get(chunk.plainChunk.world) ?: return
        plugin.logger.info("Adding marker for chunk ${chunk.plainChunk.world}:${chunk.plainChunk.x},${chunk.plainChunk.z}")

        val layer = world.layerRegistry.getOrDefault(
            "visualclaim",
            SimpleLayer(
                "visualclaim",
                { "Claims" }
            )
        ) as SimpleLayer

        val hoverText = getHoverText(claim, player)

        val bx: Int = chunk.plainChunk.x * 16
        val bz: Int = chunk.plainChunk.z * 16

        val rect = Rectangle.of(markerKey(chunk), bx.toDouble(), bz.toDouble(), bx.toDouble() + 16, bz.toDouble() + 16)
        val opts = Options.builder()
            .tooltipContent(hoverText)
            .stroke(Stroke(strokeWeight, strokeColor))
            .fill(Fill(fillColor))
            .build()
        rect.options = opts
        layer.addMarker(rect)

        world.layerRegistry.register(layer) // ensure present

    }

    private fun getHoverText(claim: VCClaim, player: VCPlayer): String {
        return plugin.cfg.get("Pl3xMap.hover-text.named-claim")
            .toString()
            .replace("<owner>", player.name)
            .replace("<claim-name>", claim.displayName)

    }

    override fun removeClaimMarker(claim: VCClaim) {
        //get all chunks of the claim
        val chunks = plugin.claimService.getChunksOfClaim(claim)

        //remove marker for each chunk
        chunks.forEach {
            removeChunkMarker(it)
        }
    }

    override fun removeChunkMarker(chunk: VCChunk) {
        //get world
        val world = Pl3xMap.api().worldRegistry.get(chunk.plainChunk.world) ?: return

        val layer = world.layerRegistry.get("visualclaim")
        if (layer is SimpleLayer) {
            layer.removeMarker(markerKey(chunk))
        }
    }

    override fun removeChunkMarker(chunks: List<VCChunk>) {
        //get world
        chunks.forEach {
            removeChunkMarker(it)
        }
    }

    override fun shutdown() {
        // optional: Layer aufräumen
    }

    private fun markerKey(chunk: VCChunk): String {
        return chunk.plainChunk.world + ":" + chunk.plainChunk.x + "," + chunk.plainChunk.z + ":" + chunk.claimKey.value
    }
}