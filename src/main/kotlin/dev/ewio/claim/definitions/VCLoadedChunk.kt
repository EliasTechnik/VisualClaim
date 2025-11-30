package dev.ewio.claim.definitions

/**
 * Since the plugin doubles as a chunk loader, we need to keep track of loaded chunks.
 *
 * @property key The unique identifier for this loaded chunk record.
 * @property playerKey The unique identifier of the player who loaded the chunk.
 * @property playerLocation The location of the player when the chunk was loaded. (So a player is able to find their loaded chunks)
 * @property chunk The actual chunk data.
 * @property firstLoaded The timestamp when the chunk was first loaded. (For ordering reasons)
 * @property name The name given to the loaded chunk by the player.
 */
data class VCLoadedChunk(
    val key: Int,
    val playerKey: Int,
    val playerLocation: VCPoint,
    val chunk: PlainChunk,
    val firstLoaded: Long,
    val name: String
){
    override fun toString(): String {
        return "VCLoadedChunk(key=$key, playerKey=$playerKey, playerLocation=$playerLocation, firstLoaded=$firstLoaded, name='$name', chunk=$chunk)"
    }
}
