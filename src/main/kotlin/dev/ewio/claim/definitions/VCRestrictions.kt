package dev.ewio.claim.definitions

data class VCRestrictions(
    val maxClaims: Int, //the maximum number of claims a player can have (-1 = unlimited)
    val maxChunks: Int, //the maximum number of chunks a player can claim (-1 = unlimited)
    val maxClaimNameLength: Int, //the maximum length of a claim name
    val listOtherPlayerClaims: Boolean = false, //whether the player can list other players' claims
    val canClaim: Boolean = false, //whether the player can claim chunks
    val unclaimOther: Boolean = false, //whether the player can unclaim other players' chunks
    val deleteclaimOther: Boolean = false, //whether the player can delete other players' claims
    val renameOtherPlayerClaims: Boolean = false, //whether the player can rename other players' claims
    val listOtherPlayerChunkLoader: Boolean = false, //whether the player can list other players' chunk loaders
    val canLoadChunks: Boolean = false, //whether the player can load chunks
    val maxChunkLoaders: Int, //the maximum number of chunk loaders a player can have (-1 = unlimited)

)