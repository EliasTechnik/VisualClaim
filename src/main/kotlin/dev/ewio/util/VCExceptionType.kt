package dev.ewio.util

enum class VCExceptionType {
    NONE,
    CHUNK_ALREADY_CLAIMED_BY_SAME_CLAIM, //of same player
    CHUNK_CLAIMED_BY_OTHER_PLAYER,
    VCCHUNK_NOT_FOUND,
    OWNER_OF_CLAIMED_CHUNK_NOT_FOUND,
    CLAIM_BELONGS_TO_OTHER_PLAYER;

    fun toReadableString(): String {
        return toReadableString(this)
    }

    companion object{
        fun toReadableString(type: VCExceptionType): String {
            return when(type){
                NONE -> "No Exception"
                CHUNK_ALREADY_CLAIMED_BY_SAME_CLAIM -> "Chunk already claimed by the same claim"
                CHUNK_CLAIMED_BY_OTHER_PLAYER -> "Chunk is claimed by another player"
                VCCHUNK_NOT_FOUND -> "VCChunk not found in database"
                OWNER_OF_CLAIMED_CHUNK_NOT_FOUND -> "Owner of claimed chunk not found"
                CLAIM_BELONGS_TO_OTHER_PLAYER -> "Claim belongs to another player"
            }
        }
    }
}

fun getMostSevereExceptionType(types: List<VCExceptionType>): VCExceptionType {
    return when {
        types.contains(VCExceptionType.CLAIM_BELONGS_TO_OTHER_PLAYER) -> VCExceptionType.CLAIM_BELONGS_TO_OTHER_PLAYER
        types.contains(VCExceptionType.CHUNK_CLAIMED_BY_OTHER_PLAYER) -> VCExceptionType.CHUNK_CLAIMED_BY_OTHER_PLAYER
        types.contains(VCExceptionType.OWNER_OF_CLAIMED_CHUNK_NOT_FOUND) -> VCExceptionType.OWNER_OF_CLAIMED_CHUNK_NOT_FOUND
        types.contains(VCExceptionType.VCCHUNK_NOT_FOUND) -> VCExceptionType.VCCHUNK_NOT_FOUND
        types.contains(VCExceptionType.CHUNK_ALREADY_CLAIMED_BY_SAME_CLAIM) -> VCExceptionType.CHUNK_ALREADY_CLAIMED_BY_SAME_CLAIM
        else -> VCExceptionType.NONE
    }
}