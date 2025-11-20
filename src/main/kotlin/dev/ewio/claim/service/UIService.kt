package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCClaimDisplayData
import dev.ewio.claim.definitions.VCPlayerContext
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.entity.Player

/**
 * Service for handling UI related operations like boss bars, titles, action bars, etc.
 */

data class VCUIBossBarData(
    val targetPlayer: Player,
    val bossBar: BossBar,
    val audience: Audience,
    val showBossBar: Boolean
)


class UIService(
    private val cc: CentralCache,
    private val ms: MessageService,
    private val getStringFromConfig: (key: String) -> String?
) {
                            // uuid, VCUIBossBarData
    val bossBarMap: MutableMap<String, VCUIBossBarData> = mutableMapOf()
    var bossBarColor: BossBar.Color
    var bossBarStyle: BossBar.Overlay


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
}