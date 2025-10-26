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
        val maxClaims = getUpperLimitFromPermission(bukkitPlayer, "vc.restrictions.maxclaims.") ?: defaultVCRestrictions.maxClaims
        val maxChunks = getUpperLimitFromPermission(bukkitPlayer, "vc.restrictions.maxchunks.") ?: defaultVCRestrictions.maxChunks
        val maxClaimNameLength = getUpperLimitFromPermission(bukkitPlayer, "vc.restrictions.maxclaimnamelength.") ?: defaultVCRestrictions.maxClaimNameLength

        return VCRestrictions(
            maxClaims = maxClaims,
            maxChunks = maxChunks,
            maxClaimNameLength = maxClaimNameLength
        )
    }


}