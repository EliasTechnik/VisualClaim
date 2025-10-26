package dev.ewio.claim.definitions

data class VCRestrictions(
    val maxClaims: Int,
    val maxChunks: Int,
    val maxClaimNameLength: Int,
    val listOtherPlayerClaims: Boolean = false,
    val canClaim: Boolean = false
)