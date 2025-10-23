package dev.ewio.map

import dev.ewio.claim.repository.definitions.VCChunk
import dev.ewio.claim.repository.definitions.VCClaim

class NoopMapService: MapService {
    override fun isActive(): Boolean {
        return false
    }

    override fun writeClaimMarker(claim: VCClaim) {
        // No operation
    }

    override fun removeClaimMarker(claim: VCClaim) {
        // No operation
    }

    override fun shutdown() {
        // No operation
    }

    override fun removeChunkMarker(chunk: VCChunk) {
        // No operation
    }

    override fun removeChunkMarker(chunks: List<VCChunk>) {
        // No operation
    }
}