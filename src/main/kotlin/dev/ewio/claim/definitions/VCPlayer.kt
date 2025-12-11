package dev.ewio.claim.definitions

import java.util.UUID

data class VCPlayer(
    val key: Int = -1,
    val mcUUID: UUID,
    val name: String,
    val resolvedNameAt: Long,
    val autoClaim: Boolean = false,
    val autoClaimTargetClaimKey: Int = -1,
    val bossbar: Boolean = true,
    val showLore: Boolean = true,
    val additionalData: String = "", // for future use
){
    override fun toString(): String {
        return "VCPlayer(key=$key, mcUUID=$mcUUID, name='$name', resolvedNameAt=$resolvedNameAt, autoClaim=$autoClaim, autoClaimTargetClaimKey=$autoClaimTargetClaimKey, bossbar=$bossbar, showLore=$showLore, additionalData='$additionalData')"
    }
}
