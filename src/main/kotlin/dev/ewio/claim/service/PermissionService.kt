package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCPlayer
import dev.ewio.claim.definitions.VCRestrictions
import dev.ewio.util.log
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachmentInfo

class PermissionService(
    val defaultVCRestrictions: VCRestrictions,
    val triggerWords: List<String>,
    val forbiddenCharset: List<Char>
) {

    private fun getPermissionSuffix(realPlayer: Player, permissionPrefix: String):String? {
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

    fun hasPermission(realPlayer: Player, permissionPrefix: String):Boolean {
        for (pai: PermissionAttachmentInfo in realPlayer.effectivePermissions) {
            if (!pai.value) continue // nur gesetzte/true Nodes
            val node = pai.permission.lowercase()
            if (node.startsWith(permissionPrefix.lowercase())) {
                return true
            }
        }
        return false
    }

    fun getUpperLimitFromPermission(realPlayer: Player, permissionPrefix: String):Int?{
        val perm = getPermissionSuffix(realPlayer, permissionPrefix)
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

    fun isNameAllowed(claimName: String):Boolean{
        //checks if a name is allowed and does not conflict with command keywords
        return (!triggerWords.any { it.equals(claimName, ignoreCase = true) } && !claimName.any { forbiddenCharset.contains(it) })
    }

    fun isLoreAllowed(newDescription: String): Boolean {
        //checks if a lore is allowed
        return !newDescription.any { forbiddenCharset.contains(it) }
    }

    fun getRestrictionsForPlayer(player: VCPlayer, bukkitPlayer: Player): VCRestrictions {
        val canClaim = hasPermission(bukkitPlayer, "visualclaim.claim")
        val listOtherPlayerClaims = hasPermission(bukkitPlayer, "visualclaim.listOther")
        val maxClaims = getUpperLimitFromPermission(bukkitPlayer, "visualclaim.maxclaims.") ?: defaultVCRestrictions.maxClaims
        val maxChunks = getUpperLimitFromPermission(bukkitPlayer, "visualclaim.maxchunks.") ?: defaultVCRestrictions.maxChunks
        val maxClaimNameLength = getUpperLimitFromPermission(bukkitPlayer, "visualclaim.maxclaimnamelength.") ?: defaultVCRestrictions.maxClaimNameLength
        val unclaimOther = hasPermission(bukkitPlayer, "visualclaim.unclaimOther")
        val deleteclaimOther = hasPermission(bukkitPlayer, "visualclaim.deleteOther")
        val renameOtherPlayerClaims = hasPermission(bukkitPlayer, "visualclaim.renameOther")
        val canLoadChunks = hasPermission(bukkitPlayer, "visualclaim.loadChunks")
        val listOtherPlayerChunkLoader = hasPermission(bukkitPlayer, "visualclaim.listOtherChunkLoader")
        val maxChunkLoaders = getUpperLimitFromPermission(bukkitPlayer, "visualclaim.maxchunkloaders.") ?: defaultVCRestrictions.maxChunkLoaders
        val canUnloadOtherChunks = hasPermission(bukkitPlayer, "visualclaim.unloadChunksOther")
        val maxClaimLoreLength = getUpperLimitFromPermission(bukkitPlayer, "visualclaim.maxclaimlorelength.") ?: defaultVCRestrictions.maxClaimNameLength


        return VCRestrictions(
            maxClaims = maxClaims,
            maxChunks = maxChunks,
            maxClaimNameLength = maxClaimNameLength,
            listOtherPlayerClaims = listOtherPlayerClaims,
            canClaim = canClaim,
            unclaimOther = unclaimOther,
            deleteclaimOther = deleteclaimOther,
            renameOtherPlayerClaims = renameOtherPlayerClaims,
            listOtherPlayerChunkLoader = listOtherPlayerChunkLoader,
            canLoadChunks = canLoadChunks,
            maxChunkLoaders = maxChunkLoaders,
            canUnloadOtherChunks = canUnloadOtherChunks,
            maxClaimLoreLength = maxClaimLoreLength
        )
    }


}