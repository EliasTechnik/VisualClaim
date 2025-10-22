package dev.ewio.map

class NoopMapService: MapService {
    override fun isActive(): Boolean {
        return false
    }

    override fun writeClaimMarker(claim: dev.ewio.claim.VCClaim) {
        // No operation
    }

    override fun removeClaimMarker(claim: dev.ewio.claim.VCClaim) {
        // No operation
    }

    override fun shutdown() {
        // No operation
    }

    override fun removeChunkMarker(chunk: dev.ewio.claim.VCChunk) {
        // No operation
    }

    override fun removeChunkMarker(chunks: List<dev.ewio.claim.VCChunk>) {
        // No operation
    }
}