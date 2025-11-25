package dev.ewio.claim.definitions

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
        fun fromKey(key: String): PlainChunk? {
            val parts = key.split(":")
            if(parts.size != 3) return null
            val world = parts[0]
            val x = parts[1].toIntOrNull() ?: return null
            val z = parts[2].toIntOrNull() ?: return null
            return PlainChunk(world, x, z)
        }
    }

    override fun toString(): String {
        return "PlainChunk(world='$world', x=$x, z=$z)"
    }
}

data class VCChunk(
    val key: Int = -1,
    val claimKey: Int,
    val plainChunk: PlainChunk,
) {
    companion object {
        fun dummy(): VCChunk = VCChunk(
            -1,
            -1,
            PlainChunk(
                "dummy_world",
                0,
                0
            )
        )
    }

    override fun toString(): String {
        return "VCChunk(key=$key, claimKey=$claimKey, plainChunk=$plainChunk)"
    }
}
