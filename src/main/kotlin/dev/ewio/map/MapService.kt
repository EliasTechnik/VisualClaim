package dev.ewio.map

import dev.ewio.claim.repository.definitions.VCChunk
import dev.ewio.claim.repository.definitions.VCClaim

interface MapService {
    fun isActive(): Boolean
    fun writeClaimMarker(claim: VCClaim)
    fun removeClaimMarker(claim: VCClaim)
    fun removeChunkMarker(chunks: List<VCChunk>)
    fun removeChunkMarker(chunk: VCChunk)
    fun shutdown()
}