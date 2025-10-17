package dev.ewio.claim

import dev.ewio.util.CMDStringWrapper
import dev.ewio.util.UKey

data class VCClaim(
    val key: UKey<VCClaim>,
    val playerKey: UKey<VCPlayer>,
    val displayName: CMDStringWrapper,
    val isDefaultClaim: Boolean = false
)

