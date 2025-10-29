package dev.ewio.claim.service

import dev.ewio.annotations.Costly
import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.claim.definitions.VCResult
import dev.ewio.util.SimpleCache
import dev.ewio.util.log
import dev.ewio.util.logSevere
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

        log("player ${context.player.name} (${context.player.mcUUID}) at chunk X:${chunk.x} Z:${chunk.z} in world ${chunk.world} with claim name '$claimName' issued claim command.")

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

            //check if chunk is already part of a claim
            val exVCChunk = claimService.getVCChunkByPlainChunk(chunk)
            if (exVCChunk != null) {
                //it is already part of a claim
                //check if it is owned by the same player
                return if (context.claims.firstOrNull { it.key == exVCChunk.claimKey } != null) {
                    //the chunk is already claimed by the same claim
                    VCResult.CreateClaim.ChunkAlreadyClaimedBySameClaim
                } else VCResult.CreateClaim.ChunkClaimedByOtherPlayer(
                    otherPlayer = claimService.getVCChunkOwner(exVCChunk)?.name
                        ?: "unknown"
                )
            }else{
                val vcChunk = claimService.appendChunkToExistingClaim(
                    chunk = chunk,
                    claim = existingClaim,
                    player = context.player
                )
                return if(vcChunk == null){
                    VCResult.CreateClaim.ChunkCouldNotBeClaimed
                } else {
                    VCResult.CreateClaim.ChunkClaimedSucessfully
                }
            }
        }
    }

    suspend fun unclaimChunk(
        context: VCPlayerContext,
        chunk: PlainChunk,
        forceUnclaim: Boolean = false
    ): VCResult {
        //find chunk

        val vcChunk = claimService.getVCChunkByPlainChunk(chunk)
            ?: return VCResult.UnclaimChunk.UnclaimAlreadyUnclaimed

        //check owner
        val claim = context.claims.firstOrNull { it.key == vcChunk.claimKey }
        if(claim != null){
            //the player owns this chunk
            //proceed to unclaim
            return when( updatePlayerContextCache(context, {claimService.removeChunkFromClaim(vcChunk)})){
                is VCResult.UnclaimChunk.UnclaimSuccessful -> VCResult.UnclaimChunk.UnclaimSuccessful(
                    claimName = claim.displayName
                )
                is VCResult.UnclaimChunk.UnclaimAlreadyUnclaimed -> VCResult.UnclaimChunk.UnclaimAlreadyUnclaimed
                else -> VCResult.UnknownFailure
            }
        }else{
            //the player does not own this chunk
            if(forceUnclaim && context.restrictions.unclaimOther){
                //proceed to unclaim
                return updatePlayerContextCache(context, { claimService.removeChunkFromClaim(vcChunk) })
            }else{
                val owner = claimService.getOwnerOfChunk(vcChunk)

                return VCResult.UnclaimChunk.UnclaimFailedWrongOwner(
                    ownerName = owner?.name ?: "unknown"
                )
            }
        }
    }

    suspend fun deleteClaim(
        context: VCPlayerContext,
        claimName: String,
        pretestAdmin:Boolean = false, //if true, just check if the claim exists
        playerName: String = "",
        adminMode: Boolean = false
    ): VCResult {
        if(adminMode){
            //the player is trying to delete another player's claim
            //get claim
            val claim = claimService.getClaimByNameAndPlayerName(
                claimName = claimName,
                playerName = playerName
            ) ?: return VCResult.DeleteClaim.VCClaimNotFound(claimName)

            if(pretestAdmin){
                return VCResult.DeleteClaim.ConfirmOtherPlayerClaimRequired(claimName)
            }

            //check permission
            return if(context.restrictions.deleteclaimOther){
                updatePlayerContextCache(context, {claimService.deleteClaim(claim)})
            }else{
                VCResult.DeleteClaim.NotOwnerOfClaim(claimName)
            }
        } else {
            //normal deletion
            val claim = context.claims.firstOrNull { it.displayName == claimName }
                ?: return VCResult.DeleteClaim.VCClaimNotFound(claimName)
            return updatePlayerContextCache(context, {claimService.deleteClaim(claim)})

        }
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

    suspend fun getClaimAtChunk(
        chunk: PlainChunk
    ): VCResult {
        val claim = claimService.getClaimAtChunk(chunk)

        if(claim == null){
            return VCResult.ClaimInfo.ChunkNotClaimed
        }else{
            val owner = claimService.getPlayerByKey(claim.playerKey)
            return if(owner != null){
                VCResult.ClaimInfo.chunkClaimed(
                    claimName = claim.displayName,
                    ownerName = owner.name
                )
            }else{
                logSevere("Claim at chunk X:${chunk.x} Z:${chunk.z} in world ${chunk.world} has no valid owner! ClaimKey: ${claim.key}, PlayerKey: ${claim.playerKey}. VC Database might be in an inconsistent state.")
                VCResult.ClaimInfo.ClaimedButWithoutOwner //this is an error that would only happen if the database is in an inconsistent state
            }
        }

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

    suspend fun getFreshPlayerContext(context: VCPlayerContext): VCPlayerContext? {
        val updatedContext = claimService.getPlayerContextByKey(context.player.key) ?: return null
        return VCPlayerContext(
            restrictions = context.restrictions,
            dbContext = updatedContext
        )
    }

    private suspend fun <T> updatePlayerContextCache(context: VCPlayerContext, exFirst: suspend () -> T): T  {
        val result = exFirst()
        val updatedContext = this.getFreshPlayerContext(context)?: return result
        contextCache.put(updatedContext.player.mcUUID, updatedContext)
        return result
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

    @Costly
    suspend fun getClaimNamesForPlayer(playerName: String): List<String> {
        val player = claimService.getPlayerByName(playerName)

        if(player != null){
            val contexts = claimService.getPlayerContextByKey(player.key)
            if(contexts != null){
                return contexts.claims.map { it.displayName }
            }
        }
        return emptyList()
    }

}