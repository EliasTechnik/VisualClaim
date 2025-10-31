package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCPlayer
import dev.ewio.claim.definitions.VCRestrictions
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachmentInfo

class PermissionService(
    val defaultVCRestrictions: VCRestrictions
) {

    fun getPermission(realPlayer: Player, permissionPrefix: String):String? {
        // Alle effektiven (expandierten) Nodes durchsuchen
        for (pai: PermissionAttachmentInfo in realPlayer.effectivePermissions) {
            if (!pai.value) continue // nur gesetzte/true Nodes
            val node = pai.permission.lowercase()

            if (node.startsWith(permissionPrefix.lowercase())) {
                val suffix = node.removePrefix(permissionPrefix)
                return suffix.ifEmpty { null }
            }
        }
        return null
    }

    fun getUpperLimitFromPermission(realPlayer: Player, permissionPrefix: String):Int?{
        val perm = getPermission(realPlayer, permissionPrefix)
        perm?.let{
            if(it == "*" || it == "unlimited" || it == "-1"){
                return -1
            }
            else{
                return perm.toIntOrNull()
            }
        }
        return null
    }

    fun getRestrictionsForPlayer(player: VCPlayer, bukkitPlayer: Player): VCRestrictions {
        val maxClaims = getUpperLimitFromPermission(bukkitPlayer, "VisualClaim.maxclaims.") ?: defaultVCRestrictions.maxClaims
        val maxChunks = getUpperLimitFromPermission(bukkitPlayer, "VisualClaim.maxchunks.") ?: defaultVCRestrictions.maxChunks
        val maxClaimNameLength = getUpperLimitFromPermission(bukkitPlayer, "VisualClaim.maxclaimnamelength.") ?: defaultVCRestrictions.maxClaimNameLength
        val listOtherPlayerClaims = getPermission(bukkitPlayer, "VisualClaim.listOther") != null
        val canClaim = getPermission(bukkitPlayer, "VisualClaim.claim") != null
        val unclaimOther = getPermission(bukkitPlayer, "VisualClaim.unclaimOther") != null
        val deleteclaimOther = getPermission(bukkitPlayer, "VisualClaim.deleteOther") != null
        val renameOtherPlayerClaims = getPermission(bukkitPlayer, "VisualClaim.renameOther") != null


        return VCRestrictions(
            maxClaims = maxClaims,
            maxChunks = maxChunks,
            maxClaimNameLength = maxClaimNameLength,
            listOtherPlayerClaims = listOtherPlayerClaims,
            canClaim = canClaim,
            unclaimOther = unclaimOther,
            deleteclaimOther = deleteclaimOther,
            renameOtherPlayerClaims = renameOtherPlayerClaims
        )
    }


}