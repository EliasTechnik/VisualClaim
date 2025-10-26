package dev.ewio.claim.service

import dev.ewio.annotations.Costly
import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.claim.definitions.VCResult
import dev.ewio.util.SimpleCache
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PrerequisiteService(
    private val claimService: ClaimService,
    private val permissionService: PermissionService,
    private val coroutineScope: CoroutineScope
) {

    private val contextCache = ContextCache(
        fetch = { uuid ->
            this.getPlayerContext(uuid)
        },
        coroutineScope = coroutineScope
    )

    private val playerNameCache = SimpleCache<String>(
        fetchAll = {
            claimService.getAllPlayers().map { it.name }
        },
        coroutineScope = coroutineScope
    )

    /**
     * The createClaim function can be used to create a new claim or append a chunk to an existing claim.
     * It will also transfer a chunk from one existing claim to a new claim if the chunk is already claimed
     * by another claim of the same player.
     */

    suspend fun createClaim(
        context: VCPlayerContext,
        chunk: PlainChunk,
        claimName: String = "" //if no name is given the claim is appended to the last modified claim
    ): VCResult {

        log("Creating claim for player ${context.player.name} (${context.player.mcUUID}) at chunk X:${chunk.x} Z:${chunk.z} in world ${chunk.world} with claim name '$claimName'")

        //Check if max chunks reached
        if(context.chunks.size >= context.restrictions.maxChunks && context.restrictions.maxChunks != -1) {
            log("Player ${context.player.name} (${context.player.mcUUID}) has reached the maximum number of chunks: ${context.restrictions.maxChunks}")
            return VCResult.CreateClaim.ChunkLimitReached(context.restrictions.maxChunks)
        }

        if(claimName.length > context.restrictions.maxClaimNameLength && context.restrictions.maxClaimNameLength != -1) {
            log("Player ${context.player.name} (${context.player.mcUUID}) provided a claim name that is too long: $claimName")
            return VCResult.CreateClaim.ClaimNameTooLong(context.restrictions.maxClaimNameLength)
        }

        //check if append to last modified claim
        if(claimName.isEmpty()){
            //append - get last modified claim
            val lastModifiedClaim = context.claims.maxByOrNull { it.lastModified }
            return if(lastModifiedClaim == null) {
                log("Player ${context.player.name} (${context.player.mcUUID}) has no existing claims to append to.")
                VCResult.CreateClaim.NoExistingClaimFound
            }else{
                //append to the claim
                val vcChunk = claimService.appendChunkToExistingClaim( chunk, lastModifiedClaim, context.player)
                if(vcChunk == null){
                    log("Chunk X:${chunk.x} Z:${chunk.z} in world ${chunk.world} could not be appended to claim ${lastModifiedClaim.displayName} by player ${context.player.name} (${context.player.mcUUID})")
                    VCResult.CreateClaim.ChunkCouldNotBeClaimed
                } else {
                    VCResult.CreateClaim.ChunkClaimedSucessfully
                }
            }
        }

        //check if new claim
        val existingClaim = context.claims.firstOrNull { it.displayName == claimName }
        if(existingClaim == null) {
            //new claim

            //check if max claims reached
            if (context.claims.size >= context.restrictions.maxClaims && context.restrictions.maxClaims != -1) {
                log("Player ${context.player.name} (${context.player.mcUUID}) has reached the maximum number of claims: ${context.restrictions.maxClaims}")
                return VCResult.CreateClaim.ClaimLimitReached(context.restrictions.maxClaims)
            }

            log("No existing claim with name '$claimName' found for player ${context.player.name} (${context.player.mcUUID}). Creating new (empty) claim.")
            //create empty claim first
            val claim = claimService.createEmptyClaim(
                player = context.player,
                claimName = claimName
            )

            if (claim == null) {
                log("Claim '${claimName}' could not be created for player ${context.player.name} (${context.player.mcUUID}). The service returned no claim.")
                return VCResult.CreateClaim.ClaimCouldNotBeCreated
            } else {
                //add chunk
                //check if chunk is already part of a claim
                val exVCChunk = claimService.getVCChunkByPlainChunk(chunk)
                if (exVCChunk != null) {
                    //it is already part of a claim
                    //check if it is owned by the same player
                    if (context.claims.firstOrNull { it.key == exVCChunk.claimKey } != null) {
                        //transfer chunk to new claim
                        val transferResult = claimService.transferChunkToAnotherClaim(
                            chunk = exVCChunk,
                            targetClaim = claim,
                            player = context.player
                        )
                        return when (transferResult) {
                            is VCResult.TransferChunk.TransferSuccessful -> VCResult.CreateClaim.ClaimCreatedSuccessfully
                            is VCResult.TransferChunk.VCChunkNotFound -> VCResult.CreateClaim.UNKNOWN
                            else -> {
                                VCResult.CreateClaim.UNKNOWN
                            }
                        }
                    } else return VCResult.CreateClaim.ChunkClaimedByOtherPlayer(
                        otherPlayer = claimService.getVCChunkOwner(exVCChunk)?.name
                            ?: "unknown"
                    )
                } else {
                    //just append chunk to new claim
                    val vcChunk = claimService.appendChunkToExistingClaim(
                        chunk = chunk,
                        claim = claim,
                        player = context.player
                    )
                    if(vcChunk == null){
                        return VCResult.CreateClaim.ChunkCouldNotBeClaimed
                    } else {
                        return VCResult.CreateClaim.ChunkClaimedSucessfully
                    }
                }
            }
        }
        else{
            //there is already a claim with this name - append to it
            val vcChunk = claimService.appendChunkToExistingClaim(
                chunk = chunk,
                claim = existingClaim,
                player = context.player
            )
            if(vcChunk == null){
                return VCResult.CreateClaim.ChunkCouldNotBeClaimed
            } else {
                return VCResult.CreateClaim.ChunkClaimedSucessfully
            }
        }
    }

    suspend fun unclaimChunk(
        context: VCPlayerContext,
        chunk: PlainChunk
    ): VCResult {
        //find chunk in context
        val vcChunk = context.chunks.firstOrNull {
            it.plainChunk.world == chunk.world &&
            it.plainChunk.x == chunk.x &&
            it.plainChunk.z == chunk.z
        }

        if(vcChunk == null) {
            return VCResult.RemoveChunk.VCChunkNotFound
        }

        return claimService.removeChunkFromClaim(vcChunk)
    }

    suspend fun deleteClaim(
        context: VCPlayerContext,
        claimName: String
    ): VCResult {
        val claim = context.claims.firstOrNull { it.displayName == claimName }
            ?: return VCResult.DeleteClaim.VCClaimNotFound

        return claimService.deleteClaim(claim)
    }

    suspend fun renameClaim(
        context: VCPlayerContext,
        oldName: String,
        newName: String,
    ): VCResult {
        val claim = context.claims.firstOrNull { it.displayName == oldName }
            ?: return VCResult.RenameClaim.VCClaimNotFound

        if(newName.length > context.restrictions.maxClaimNameLength && context.restrictions.maxClaimNameLength != -1) {
            return VCResult.RenameClaim.ClaimNameTooLong(context.restrictions.maxClaimNameLength)
        }

        return claimService.renameClaim(
            claim = claim,
            newName = newName,
            player = context.player
        )
    }

    suspend fun getPlayerContext(sender: CommandSender): Pair<VCPlayerContext, Player>? {
        val realPlayer = sender as? Player ?: return null
        log("Fetching player context for ${realPlayer.name} (${realPlayer.uniqueId})")
        getPlayerContext(realPlayer)?.let{
            contextCache.put(realPlayer.uniqueId, it)
            return Pair(it, realPlayer)
        }
        return null
    }

    private suspend fun getPlayerContext(player: Player): VCPlayerContext? {
        val context = claimService.registerPlayerContextByUUID(player.uniqueId)?: return null
        return VCPlayerContext(
                restrictions = permissionService.getRestrictionsForPlayer(
                    player = context.player,
                    bukkitPlayer = player
                ),
                dbContext = context,
            )
    }

    suspend fun updatePlayerContext(context: VCPlayerContext): VCPlayerContext? {
        val updatedContext = claimService.getPlayerContextByKey(context.player.key) ?: return null
        return VCPlayerContext(
            restrictions = context.restrictions,
            dbContext = updatedContext
        )
    }

    fun getCachedPlayerContext(player: Player): VCPlayerContext? {
        return contextCache.get(player)
    }

    @Costly
    suspend fun getPlayerNames(): List<String> {
        return claimService.getAllPlayers().map { it.name }
    }

    fun getCachedPlayerNames(): List<String> {
        return playerNameCache.getAll()
    }

}