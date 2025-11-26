package dev.ewio.util

import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCEdge2D

object EdgeHelper {

    /**
     * Given a list of VCChunks, determine the edges of the claim in the specified world.
     * An edge is defined as a side of a chunk that does not have a neighboring chunk in the claim.
     *
     * @param chunks The list of VCChunks that make up the claim.
     * @return A list of VCEdge2D representing the edges of the claim.
     */
    fun getEdgeOfClaim(chunks: List<VCChunk>): List<VCEdge2D>{
        val chunkSet = chunks.map { it.plainChunk.toKey() }
        val edges = mutableListOf<VCEdge2D>()

        if(chunkSet.isEmpty()){
            log("No chunks provided to determine edges.")
            return edges
        }

        chunks.forEach { chunk ->
            //check each side for neighboring chunks
            //left side
            val leftNeighborKey = chunk.plainChunk.toKey(-1, 0)
            if(!chunkSet.contains(leftNeighborKey)) {
                //no neighbor on left side, add left edge
                edges.add(
                    VCEdge2D(
                        world = chunk.plainChunk.world,
                        start = chunk.plainChunk.getLowerLeftCornerBlock(),
                        end = chunk.plainChunk.getUpperLeftCornerBlock()
                    )
                )
            }

            //right side
            val rightNeighborKey = chunk.plainChunk.toKey(1, 0)
            if(!chunkSet.contains(rightNeighborKey)) {
                //no neighbor on right side, add right edge
                edges.add(
                    VCEdge2D(
                        world = chunk.plainChunk.world,
                        start = chunk.plainChunk.getUpperRightCornerBlock(),
                        end = chunk.plainChunk.getLowerRightCornerBlock()
                    )
                )
            }

            //top side
            val topNeighborKey = chunk.plainChunk.toKey(0, 1)
            if(!chunkSet.contains(topNeighborKey)) {
                //no neighbor on top side, add top edge
                edges.add(
                    VCEdge2D(
                        world = chunk.plainChunk.world,
                        start = chunk.plainChunk.getUpperLeftCornerBlock(),
                        end = chunk.plainChunk.getUpperRightCornerBlock()
                    )
                )
            }

            //bottom side
            val bottomNeighborKey = chunk.plainChunk.toKey(0, -1)
            if(!chunkSet.contains(bottomNeighborKey)) {
                //no neighbor on bottom side, add bottom edge
                edges.add(
                    VCEdge2D(
                        world = chunk.plainChunk.world,
                        start = chunk.plainChunk.getLowerRightCornerBlock(),
                        end = chunk.plainChunk.getLowerLeftCornerBlock()
                    )
                )
            }
        }
        return edges
    }

}