package dev.ewio.util

import dev.ewio.claim.VCChunk
import dev.ewio.claim.VCClaim
import dev.ewio.claim.VCPlayer

enum class uKeySpace{
    PLAYER,
    CLAIM,
    CHUNK
}

abstract class uKeyProvider<S> {
    private var nextKey = 0

    fun nextKey(): UKey<S> {
        val key = nextKey++
        return UKey(key, this as S)
    }

    fun initialize(start: Int) {
        nextKey = start
    }
}

object PlayerUKeyProvider : uKeyProvider<VCPlayer>()
object ClaimUKeyProvider : uKeyProvider<VCClaim>()
object ChunkUKeyProvider : uKeyProvider<VCChunk>()