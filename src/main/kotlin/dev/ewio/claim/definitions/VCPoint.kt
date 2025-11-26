package dev.ewio.claim.definitions
/**
 * A point in 3D space.
 *
 * @property x The X coordinate.
 * @property y The Y coordinate.
 * @property z The Z coordinate.
 */
data class VCPoint(
    val x: Double,
    val y: Double,
    val z: Double
)

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
