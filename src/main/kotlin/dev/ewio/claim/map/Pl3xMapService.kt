package dev.ewio.claim.map

import dev.ewio.VisualClaim
import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCPlayer
import dev.ewio.util.log

import net.pl3x.map.core.Pl3xMap
import net.pl3x.map.core.markers.layer.Layer
import net.pl3x.map.core.markers.layer.SimpleLayer
import net.pl3x.map.core.markers.marker.Rectangle
import net.pl3x.map.core.markers.option.Fill
import net.pl3x.map.core.markers.option.Options
import net.pl3x.map.core.markers.option.Popup
import net.pl3x.map.core.markers.option.Stroke
import net.pl3x.map.core.registry.Registry
import org.bukkit.configuration.file.FileConfiguration
import java.util.*
import java.util.function.Supplier


class Pl3xMapService: MapService {
    private var plugin: VisualClaim
    private val layers: MutableMap<UUID?, SimpleLayer?> = HashMap<UUID?, SimpleLayer?>() // pro Welt ein Layer
    private var strokeAlpha: Double = 0.7
    private var strokeWeight: Int = 0
    private var fillAlpha: Double = 0.4

    constructor(plugin: VisualClaim) {
        this.plugin = plugin
        val cfg: FileConfiguration = plugin.config
        this.strokeAlpha = cfg.getDouble("color.transparency.stroke", 0.3)
        this.fillAlpha = cfg.getDouble("color.transparency.fill", 0.4)
        this.strokeWeight = cfg.getInt("marker.stroke-weight", 2)

        // register layers per world
        val api = Pl3xMap.api()
        for (world in api.worldRegistry.values()) {
            val reg: Registry<Layer> = world.layerRegistry
            val layer = SimpleLayer("visualclaim", Supplier { plugin.messageService.getString("Pl3xMap.layer-name")})
            layer.setShowControls(true).setLiveUpdate(true).zIndex = 250
            reg.register(layer)
            layers[UUID.nameUUIDFromBytes(world.name.toByteArray())] = layer // Key based on worldname
        }
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun writeClaimMarker(player: VCPlayer, claim: VCClaim, chunks: List<VCChunk>) {
        log("Writing markers for claim ${claim.displayName} owned by ${player.name} over ${chunks.size} chunks.")

        //add marker for each chunk
        chunks.forEach {
            writeChunkbasedClaimMarker(claim, player, it)
        }
    }

    private fun writeChunkbasedClaimMarker(claim: VCClaim, player: VCPlayer, chunk: VCChunk) {
        //get world
        val world = Pl3xMap.api().worldRegistry.get(chunk.plainChunk.world) ?: return
        log("Adding marker for chunk ${chunk.plainChunk.world}:${chunk.plainChunk.x},${chunk.plainChunk.z}")

        val layer = world.layerRegistry.getOrDefault(
            "visualclaim",
            SimpleLayer(
                "visualclaim",
                { "Claims" }
            )
        ) as SimpleLayer

        val hoverText = getHoverText(claim, player)
        val popupText = claim.description

        val popup = Popup()
        popup.content = popupText.ifEmpty { hoverText }

        val fillColor = claim.color.getIntColorWithAlpha((255 * fillAlpha).toInt())
        val strokeColor = claim.color.getIntColorWithAlpha((255 * strokeAlpha).toInt())


        val bx: Int = chunk.plainChunk.x * 16
        val bz: Int = chunk.plainChunk.z * 16

        val rect = Rectangle.of(markerKey(chunk), bx.toDouble(), bz.toDouble(), bx.toDouble() + 16, bz.toDouble() + 16)
        val opts = Options.builder()
            .tooltipContent(hoverText)
            .popup(popup)
            .stroke(Stroke(strokeWeight, strokeColor))
            .fill(Fill(fillColor))
            .build()
        rect.options = opts
        layer.addMarker(rect)

        world.layerRegistry.register(layer) // ensure present

    }

    private fun getHoverText(claim: VCClaim, player: VCPlayer): String {
        return plugin.messageService.getString("Pl3xMap.hover-text.named-claim", mapOf(
            "owner" to player.name,
            "claim_name" to claim.displayName
        ))
    }



    private fun removeChunkMarker(chunk: VCChunk) {
        //get world
        val world = Pl3xMap.api().worldRegistry.get(chunk.plainChunk.world) ?: return

        val layer = world.layerRegistry.get("visualclaim")
        if (layer is SimpleLayer) {
            layer.removeMarker(markerKey(chunk))
        }
    }

    override fun removeChunkMarker(chunks: List<VCChunk>) {
        chunks.forEach {
            removeChunkMarker(it)
        }
    }

    override fun shutdown() {
        //Layer aufräumen
        for ((_, layer) in layers) {
            layer?.clearMarkers()
        }
    }

    private fun markerKey(chunk: VCChunk): String {
        return chunk.plainChunk.world + ":" + chunk.plainChunk.x + "," + chunk.plainChunk.z + ":" + chunk.claimKey
    }
}