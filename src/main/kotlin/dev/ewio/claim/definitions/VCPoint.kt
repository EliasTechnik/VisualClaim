package dev.ewio.claim.definitions
/**
 * A point in 3D space.
 *
 * @property x The X coordinate.
 * @property y The Y coordinate.
 * @property z The Z coordinate.
 */
data class VCPoint(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double
){
    fun toKey(): String {
        return "$world:$x:$y:$z"
    }

    companion object{
        fun fromKey(key: String): VCPoint? {
            val parts = key.split(":")
            if(parts.size != 4) return null
            val world = parts[0]
            val x = parts[1].toDoubleOrNull() ?: return null
            val y = parts[2].toDoubleOrNull() ?: return null
            val z = parts[3].toDoubleOrNull() ?: return null
            return VCPoint(world, x, y, z)
        }
    }
}

/**
 * A point in 2D space (X and Z coordinates).
 *
 * @property x The X coordinate.
 * @property z The Z coordinate.
 */
data class VCPoint2D(
    val x: Int,
    val z: Int
)
