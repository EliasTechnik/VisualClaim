package dev.ewio.permission

import dev.ewio.VisualClaim
import dev.ewio.claim.VCPlayer
import org.bukkit.entity.Player

class PermissionService(
    val plugin: VisualClaim
) {

    fun getMaxClaimableChunksOfPlayer(player: Player):Int{
        // For now, we just return the value from config.
        // In the future, we could integrate with a permissions plugin to get per-player values.
        return plugin.cfg.get("limits.max-chunks-per-player") as? Int ?: 5
    }
}