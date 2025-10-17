package dev.ewio.util

import dev.ewio.claim.ClaimService
import dev.ewio.claim.VCPlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun registerAndGetVCPlayer(sender: CommandSender, db: ClaimService): VCPlayer?{
    val realPlayer = sender as? Player ?: return null
    return db.registerAndGetVCPlayerByUUID(realPlayer.uniqueId)
}

fun registerAndGetVCPlayerAndRealPlayer(sender: CommandSender, db: ClaimService): Pair<VCPlayer, Player>?{
    val realPlayer = sender as? Player ?: return null
    val vcPlayer = db.registerAndGetVCPlayerByUUID(realPlayer.uniqueId)
    return Pair(vcPlayer, realPlayer)
}