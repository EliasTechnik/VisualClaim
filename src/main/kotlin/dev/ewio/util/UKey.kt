package dev.ewio.util

import dev.ewio.claim.VCPlayer

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
