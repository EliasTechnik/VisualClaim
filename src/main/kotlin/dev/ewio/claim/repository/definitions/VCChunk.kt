package dev.ewio.claim.repository.definitions

import dev.ewio.util.UKey
import org.bukkit.Chunk

data class PlainChunk(
    val world: String,
    val x: Int,
    val z: Int
){
    fun toKey(): String {
        return "$world:$x:$z"
    }

    companion object {
        fun fromBukkitChunk(chunk: Chunk): PlainChunk {
            return PlainChunk(
                chunk.world.name,
                chunk.x,
                chunk.z
            )
        }
    }
}

data class VCChunk(
    val key: UKey<VCChunk>,
    val claimKey: UKey<VCClaim>,
    val plainChunk: PlainChunk,
) {
    companion object {
        fun dummy(): VCChunk = VCChunk(
            UKey.dummy(),
            UKey.dummy(),
            PlainChunk(
                "dummy_world",
                0,
                0
            )
        )
    }
}
