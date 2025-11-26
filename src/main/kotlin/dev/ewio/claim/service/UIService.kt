package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCClaimDisplayData
import dev.ewio.claim.definitions.VCEdge
import dev.ewio.claim.definitions.VCEdge2D
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.util.EdgeHelper
import dev.ewio.util.log
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.ceil

/**
 * Service for handling UI related operations like boss bars, titles, action bars, etc.
 */




data class VCUIBossBarData(
    val targetPlayer: Player,
    val bossBar: BossBar,
    val audience: Audience,
    val showBossBar: Boolean
)

data class VCUIParticleData(
    val targetPlayer: Player,
    val particleLines: List<VCEdge2D>,
    val taskShouldStop: Boolean = false,
)


class UIService(
    private val cc: CentralCache,
    private val ms: MessageService,
    private val plugin: Plugin,
    private val getStringFromConfig: (key: String) -> String?
) {
                            // uuid, VCUIBossBarData
    val bossBarMap: MutableMap<String, VCUIBossBarData> = mutableMapOf()
    var bossBarColor: BossBar.Color
    var bossBarStyle: BossBar.Overlay
                            // uuid, VCUIParticleData
    val particleMap: MutableMap<String, VCUIParticleData> = mutableMapOf()


    init{
        val color = getStringFromConfig("BossBar.color") ?: "WHITE"
        bossBarColor = when(color.uppercase()){
            "GREEN" -> BossBar.Color.GREEN
            "BLUE" -> BossBar.Color.BLUE
            "PURPLE" -> BossBar.Color.PURPLE
            "PINK" -> BossBar.Color.PINK
            "YELLOW" -> BossBar.Color.YELLOW
            "RED" -> BossBar.Color.RED
            else -> BossBar.Color.WHITE
        }

        val style = getStringFromConfig("BossBar.style") ?: "PROGRESS"
        bossBarStyle = when(style.uppercase()) {
            "NOTCHED_6" -> BossBar.Overlay.NOTCHED_6
            "NOTCHED_10" -> BossBar.Overlay.NOTCHED_10
            "NOTCHED_12" -> BossBar.Overlay.NOTCHED_12
            "NOTCHED_20" -> BossBar.Overlay.NOTCHED_20
            else -> BossBar.Overlay.PROGRESS
        }
    }

    /**
     * Registers a boss bar receiver for the specified player.
     * It schould be called when the player joins the server or enables boss bars in settings
     *
     * @param player The player to register the boss bar receiver for.
     */
    fun registerBossBarReceiver(player: Player, context: VCPlayerContext){

        val target = Audience.audience(player)
        val bossBar = BossBar.bossBar(
            ms.getEmptyComponent(),
            1.0f,
            BossBar.Color.GREEN,
            BossBar.Overlay.PROGRESS
        )

        val data = VCUIBossBarData(
            targetPlayer = player,
            bossBar = bossBar,
            audience = target,
            showBossBar = context.player.bossbar
        )

        bossBarMap[player.uniqueId.toString()] = data
    }


    /**
     * Removes the boss bar for the specified player.
     * It schould be called when the player leaves the server or disables boss bars in settings.
     *
     * @param player The player whose boss bar receiver should be removed.
     */
    fun removeBossBarReceiver(player: Player){

        val bossBarPackage = bossBarMap[player.uniqueId.toString()] ?: return

        //hide bossbar
        bossBarPackage.audience.hideBossBar(bossBarPackage.bossBar)

        bossBarMap.remove(player.uniqueId.toString())
    }

    fun updateBossBar(player: Player, claim: VCClaimDisplayData?, context: VCPlayerContext){
        if(claim != null && context.player.bossbar) {
            //show a bossbar with claim info
            val bossBarPackage = bossBarMap[player.uniqueId.toString()] ?: return
            bossBarPackage.bossBar.name(ms.format("BossBar.title", mapOf(
                "claim_name" to claim.claim.displayName,
                "owner" to claim.ownerName
                )
            ))

            bossBarPackage.bossBar.color(bossBarColor)
            bossBarPackage.bossBar.overlay(bossBarStyle)

            bossBarPackage.audience.showBossBar(bossBarPackage.bossBar)
        }else{
            //hide bossbar
            val bossBarPackage = bossBarMap[player.uniqueId.toString()] ?: return
            bossBarPackage.audience.hideBossBar(bossBarPackage.bossBar)
        }
    }

    fun showClaimBorder(player: Player, chunksOfClaim: List<VCChunk>, claim: VCClaim){
        val chunks = chunksOfClaim//.filter{ it.plainChunk.world == player.location.world.toString()}
        if(chunksOfClaim.isEmpty()){
            log("No chunks found for claim ${claim.displayName} in world ${player.location.world?.name}, cannot show particles.")
            return
        }

        val edges = EdgeHelper.getEdgeOfClaim(chunks)

        if(edges.isEmpty()){
            log("No edges found for claim ${claim.displayName}, cannot show particles.")
            return
        }

        particleMap[player.uniqueId.toString()] = VCUIParticleData(
            targetPlayer = player,
            particleLines = edges
        )
        log("Starting particle effect for player ${player.name} around claim ${claim.displayName} with ${edges.size} edges.")

        showParticlesAroundPlayerForEdges(player, plugin)
    }


    /**
     * The Runnable task that spawns particles around the player calls this to indicate that the particles ended.
     */
    fun particlesEnded(player: Player){
        particleMap.remove(player.uniqueId.toString())
    }

    fun showParticlesAroundPlayerForEdges(
        player: Player,
        plugin: Plugin,
        uiService: UIService = this,
        durationSeconds: Int = 60
    ) {
        val ticksBetween = 16                // alle 4 ticks -> 0.2s
        val iterations = ceil(durationSeconds * 20.0 / ticksBetween).toInt()

        // Hilfsfunktion um Partikel nur für den Spieler zu spawnen
        fun spawnParticleAt(x: Int, z: Int, y: Double) {
            val loc = Location(player.location.world, x.toDouble() + 0.5, y, z.toDouble() + 0.5)
            // spawn nur beim Spieler: weniger Netzwerk, nur für den Empfänger sichtbar
            //player.spawnParticle(Particle.COPPER_FIRE_FLAME, loc, 1, 0.0, 0.0, 0.0, 0.0)
            player.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 1, 0.0, 0.0, 0.0, 0.0)

        }

        object : BukkitRunnable() {
            var runs = 0
            override fun run() {
                if (runs++ >= iterations || uiService.particleMap[player.uniqueId.toString()] == null) {
                    uiService.particlesEnded(player)
                    cancel()
                    return
                }

                val edges = uiService.particleMap[player.uniqueId.toString()]?.particleLines ?: run {
                    uiService.particlesEnded(player)
                    cancel()
                    return
                }

                val playerEyeY = player.eyeLocation.y + 5.0
                val groundLevel = player.location.y - 5.0// alternativ world.getHighestBlockYAt(x,z).toDouble()

                for (edge in edges) {
                    val startX = edge.start.x
                    val startZ = edge.start.z
                    val endX = edge.end.x
                    val endZ = edge.end.z

                    val deltaX = endX - startX
                    val deltaZ = endZ - startZ
                    val distance = Math.sqrt((deltaX * deltaX + deltaZ * deltaZ).toDouble())
                    val step = 0.5 // Abstand zwischen den Partikeln

                    val stepsCount = (distance / step).toInt()
                    for (i in 0..stepsCount) {
                        val t = i.toDouble() / stepsCount
                        val x = (startX + t * deltaX).toInt()
                        val z = (startZ + t * deltaZ).toInt()
                        // Wähle eine Y-Höhe zwischen Boden und Augenhöhe des Spielers
                        val y = groundLevel + (playerEyeY - groundLevel) * Math.random()
                        spawnParticleAt(x, z, y)
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, ticksBetween.toLong())
    }
}