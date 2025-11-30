package dev.ewio.claim.definitions

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
