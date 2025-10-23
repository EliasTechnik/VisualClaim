package dev.ewio.util

/*
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

 */