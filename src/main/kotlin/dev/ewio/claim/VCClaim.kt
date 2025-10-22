package dev.ewio.claim

import dev.ewio.util.UKey

data class VCClaim(
    val key: UKey<VCClaim>,
    val playerKey: UKey<VCPlayer>,
    val displayName: String,
    val lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        fun dummy(): VCClaim = VCClaim(
            UKey.dummy(),
            UKey.dummy(),
            "dummy",
        )
    }
}

