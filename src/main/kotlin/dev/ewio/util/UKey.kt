package dev.ewio.util

import dev.ewio.claim.repository.definitions.VCChunk
import dev.ewio.claim.repository.definitions.VCClaim
import dev.ewio.claim.repository.definitions.VCPlayer

/*
data class UKey<P>(

    val value: Int,
    val provider: P
) {
    companion object {
        fun <P> dummy(): UKey<P> {
            return UKey(-1, null as P)
        }
    }
}
 */

@JvmInline
value class UKey<P>(val value: Int) {
    companion object { fun <P> dummy() = UKey<P>(-1) }
}

fun playerKey(id: Int) = UKey<VCPlayer>(id)
fun claimKey(id: Int) = UKey<VCClaim>(id)
fun chunkKey(id: Int) = UKey<VCChunk>(id)