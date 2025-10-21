package dev.ewio.claim

import dev.ewio.util.UKey

data class PlainChunk(
    val world: String,
    val x: Int,
    val z: Int
){
    fun toKey(): String {
        return "$world:$x:$z"
    }

    companion object {
        fun fromBukkitChunk(chunk: org.bukkit.Chunk): PlainChunk {
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
    val deleted: Boolean = false
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
