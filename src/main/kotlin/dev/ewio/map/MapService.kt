package dev.ewio.map

import dev.ewio.claim.VCChunk
import dev.ewio.claim.VCClaim

interface MapService {
    fun isActive(): Boolean
    fun writeClaimMarker(claim: VCClaim)
    fun removeClaimMarker(claim: VCClaim)
    fun removeChunkMarker(chunk: VCChunk)
    fun shutdown()
}