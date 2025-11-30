package dev.ewio.claim.definitions

import dev.ewio.util.log
import org.bukkit.boss.BossBar
import java.util.UUID

/**
 * A context object that holds a VCPlayer along with their associated claims and chunks.
 *
 * @property player The VCPlayer instance.
 * @property claims A list of VCClaim instances associated with the player.
 * @property chunks A list of VCChunk instances associated with the player's claims.
 * @property chunkLoader A list of VCLoadedChunk instances associated with the player.
 * @property retrievalTimestamp The timestamp when this context was created.
 *
 * VCPlayerContext is useful for bundling together all relevant data about a player but changing values there does not
 * affect the database and should not be done!
 */

data class VCPlayerContext(
    val player: VCPlayer,
    val claims: List<VCClaim>,
    val chunks: List<VCChunk>,
    val chunkLoader: List<VCLoadedChunk>,
    val restrictions: VCRestrictions,
    val retrievalTimestamp: Long = System.currentTimeMillis()
){
    constructor(dbContext: VCPlayerDBContext, restrictions: VCRestrictions) : this(
        player = dbContext.player,
        claims = dbContext.claims,
        chunks = dbContext.chunks,
        chunkLoaders = dbContext.chunkLoader,
        restrictions = restrictions,
        retrievalTimestamp = dbContext.retrievalTimestamp
    )

    fun getAutoClaimTarget(): VCClaim? {
        val targetKey = player.autoClaimTargetClaimKey
        log("AutoClaim target key for player ${player.mcUUID} is $targetKey")
        return claims.find { it.key == targetKey }
    }

    override fun toString(): String {
        return "VCPlayerContext(player=${player}, claims=${claims.toString()}, chunks=${chunks.toString()}, chunkLoaders=${chunkLoader.toString()}restrictions=$restrictions, retrievalTimestamp=$retrievalTimestamp)"
    }

}

/**
 * A data class representing the database context for a VCPlayer.
 *
 * @property player The VCPlayer instance.
 * @property claims A list of VCClaim instances associated with the player.
 * @property chunks A list of VCChunk instances associated with the player's claims.
 * @property chunkLoader A list of VCLoadedChunk instances associated with the player.
 * @property retrievalTimestamp The timestamp when this context was created.
 *
 * VCPlayerDBContext is used for transferring data from the database to application layers. It mostly gets converted to VCPlayerContext
 * for use within the application.
 */


data class VCPlayerDBContext(
    val player: VCPlayer,
    val claims: List<VCClaim>,
    val chunks: List<VCChunk>,
    val chunkLoader: List<VCLoadedChunk>,
    val retrievalTimestamp: Long = System.currentTimeMillis()
)

/**
 * A data class representing the movement context for a VCPlayer.
 * @property uuid The UUID of the player.
 * @property usesAutoclaim Whether the player has autoclaim enabled.
 * @property usesBossbar Whether the player has bossbar enabled.
 *
 * VCMovementContext is used to track player movement settings related to claims inside the MovementService.
 */

data class VCMovementContext(
    val uuid: UUID,
    val usesAutoclaim: Boolean = false,
    val usesBossbar: Boolean = true
)



