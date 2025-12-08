package dev.ewio.claim.definitions


data class VCClaim(
    val key: Int = -1,
    val playerKey: Int,
    val displayName: String,
    val color: VCColor,
    val description: String = "",
    val lastModified: Long = System.currentTimeMillis()
) {

    override fun toString(): String {
        return "VCClaim(key=$key, playerKey=$playerKey, displayName='$displayName', lastModified=$lastModified)"
    }

    fun getDisplayData(ownerName: String): VCClaimDisplayData {
        return VCClaimDisplayData(
            claim = this,
            ownerName = ownerName
        )
    }
}

data class VCClaimDisplayData(
    val claim: VCClaim?,
    val ownerName: String?,
    val chunkloader: VCLoadedChunk? = null
)

