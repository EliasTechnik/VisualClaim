package dev.ewio.claim.definitions

/**
 * A context object that holds a VCPlayer along with their associated claims and chunks.
 *
 * @property player The VCPlayer instance.
 * @property claims A list of VCClaim instances associated with the player.
 * @property chunks A list of VCChunk instances associated with the player's claims.
 * @property retrievalTimestamp The timestamp when this context was created.
 *
 * VCPlayerContext is useful for bundling together all relevant data about a player but changing values there does not
 * affect the database and should not be done!
 */

data class VCPlayerContext(
    val player: VCPlayer,
    val claims: List<VCClaim>,
    val chunks: List<VCChunk>,
    val restrictions: VCRestrictions,
    val retrievalTimestamp: Long = System.currentTimeMillis()
){
    constructor(dbContext: VCPlayerDBContext, restrictions: VCRestrictions) : this(
        player = dbContext.player,
        claims = dbContext.claims,
        chunks = dbContext.chunks,
        restrictions = restrictions,
        retrievalTimestamp = dbContext.retrievalTimestamp
    )
}

data class VCPlayerDBContext(
    val player: VCPlayer,
    val claims: List<VCClaim>,
    val chunks: List<VCChunk>,
    val retrievalTimestamp: Long = System.currentTimeMillis()
)
