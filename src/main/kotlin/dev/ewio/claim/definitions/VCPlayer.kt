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
)
