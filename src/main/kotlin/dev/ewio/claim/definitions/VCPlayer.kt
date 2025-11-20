package dev.ewio.claim.definitions

import java.util.UUID

data class VCPlayer(
    val key: Int = -1,
    val mcUUID: UUID,
    val name: String,
    val resolvedNameAt: Long,
    val autoClaim: Boolean = false,
    val autoClaimTargetClaimKey: Int = -1,
    val bossbar: Boolean = true
){
    override fun toString(): String {
        return "VCPlayer(key=$key, mcUUID=$mcUUID, name='$name', resolvedNameAt=$resolvedNameAt, autoClaim=$autoClaim, autoClaimTargetClaimKey=$autoClaimTargetClaimKey, bossbar=$bossbar)"
    }
}
