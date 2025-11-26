package dev.ewio.claim.definitions

/**
 * Represents an edge defined by two points in 3D space.
 *
 * @property start The starting point of the edge.
 * @property end The ending point of the edge.
 */
data class VCEdge(
    val world: String,
    val start: VCPoint,
    val end: VCPoint
)
/**
 * Represents an edge defined by two points in 2D space.
 *
 * @property start The starting point of the edge.
 * @property end The ending point of the edge.
 */
data class VCEdge2D(
    val world: String,
    val start: VCPoint2D,
    val end: VCPoint2D
)
