package dev.ewio.claim.map

import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCPlayer

interface MapService {
    fun isActive(): Boolean
    fun writeClaimMarker(player: VCPlayer, claim: VCClaim, chunks: List<VCChunk>)
    fun removeChunkMarker(chunks: List<VCChunk>)
    fun shutdown()
}