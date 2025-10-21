package dev.ewio.claim

import dev.ewio.util.CMDStringWrapper
import dev.ewio.util.UKey

data class VCClaim(
    val key: UKey<VCClaim>,
    val playerKey: UKey<VCPlayer>,
    val displayName: String,
    val isDefaultClaim: Boolean = false,
    val deleted: Boolean = false
) {
    companion object {
        fun dummy(): VCClaim = VCClaim(
            UKey.dummy(),
            UKey.dummy(),
            "dummy",
            false
        )
    }
}

