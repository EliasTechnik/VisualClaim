package dev.ewio.claim.definitions


data class VCClaim(
    val key: Int = -1,
    val playerKey: Int,
    val displayName: String,
    val lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        fun dummy(): VCClaim = VCClaim(
            -1,
            -1,
            "dummy",
        )
    }
}

