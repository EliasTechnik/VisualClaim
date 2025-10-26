package dev.ewio.map

import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCPlayer

class NoopMapService: MapService {
    override fun isActive(): Boolean {
        return false
    }

    override  fun writeClaimMarker(player: VCPlayer, claim: VCClaim, chunks: List<VCChunk>) {
        // No operation
    }

    override fun shutdown() {
        // No operation
    }

    override fun removeChunkMarker(chunks: List<VCChunk>) {
        // No operation
    }
}