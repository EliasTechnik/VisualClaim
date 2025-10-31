package dev.ewio.claim.service

import dev.ewio.annotations.Costly
import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.claim.definitions.VCPlayerDBContext
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
        claimString: String = "" //if no name is given the claim is appended to the last modified claim
    ): VCResult {

        //prepare claim name
        val claimName = claimString.ifEmpty {
            context.claims.maxByOrNull { it.lastModified }?.displayName ?: ""
        }

        if(claimName.isEmpty()) return VCResult.CreateClaim.NoExistingClaimFound

        //check if the player can claim
        if (!context.restrictions.canClaim) return VCResult.MissingPermission

        //check claim name length
        if (claimName.length > context.restrictions.maxClaimNameLength && context.restrictions.maxClaimNameLength != -1) {
            log("Player ${context.player.name} (${context.player.mcUUID}) provided a claim name that is too long: $claimName")
            return VCResult.CreateClaim.ClaimNameTooLong(context.restrictions.maxClaimNameLength)
        }

        //check if chunk is claimable (eg. not reserved)
        if(isChunkReserved(chunk)) return VCResult.CreateClaim.ChunkCanNotBeClaimed

        //check if chunk is free

        val existingClaim = claimService.getClaimAtChunk(chunk)
        if(existingClaim != null){
            //the chunk is already part of a claim

            //check owner
            val owner = claimService.getOwnerOfClaim(existingClaim)
            if(owner == null) {
                //that is a db inconsistency
                error("Claim ($existingClaim) without valid owner!")
                return VCResult.UnknownFailure
            }

            if(owner.key != context.player.key) {
                //the chunk is owned by another player
                return VCResult.CreateClaim.ChunkClaimedByOtherPlayer(
                    otherPlayer = owner.name
                )
            }

            //check if the claim is the same as the target claim
            if(existingClaim.displayName == claimName) {
                //the chunk is already part of the same claim
                return VCResult.CreateClaim.ChunkAlreadyClaimedBySameClaim
            }

            //check if claim with the target name exists
            var targetClaim = context.claims.firstOrNull { it.displayName == claimName }
            if(targetClaim == null) {
                targetClaim = claimService.createEmptyClaim(
                    player = context.player,
                    claimName = claimName
                )
            }

            val vcChunk = claimService.getVCChunkByPlainChunk(chunk)

            if(vcChunk != null){
                if(targetClaim != null){
                    val transferResult = claimService.transferChunkToAnotherClaim(
                        chunk = vcChunk,
                        targetClaim = targetClaim,
                        player = context.player
                    )

                    return updatePlayerContextCache(context) {
                        when (transferResult) {
                            is VCResult.TransferChunk.TransferSuccessful -> VCResult.CreateClaim.ChunkTransferredToClaim(targetClaim, vcChunk.plainChunk)
                            is VCResult.TransferChunk.VCChunkNotFound -> VCResult.UnknownFailure
                            else -> {
                                VCResult.UnknownFailure
                            }
                        }
                    }
                }
                else{
                    return VCResult.UnknownFailure
                }
            } else {
                return VCResult.UnknownFailure
            }
        }else{
            //the chunk is free

            //check if claim with the target name exists
            var targetClaim = context.claims.firstOrNull { it.displayName == claimName }
            if(targetClaim == null) {
                //new claim
                targetClaim = claimService.createEmptyClaim(
                    player = context.player,
                    claimName = claimName
                ) ?: return VCResult.UnknownFailure
            }

            //append chunk to target claim
            val vcChunk = claimService.appendChunkToExistingClaim(
                chunk,
                targetClaim,
                context.player
            )

            return if(vcChunk == null){
                VCResult.UnknownFailure
            }else {
                updatePlayerContextCache(context) {
                    VCResult.CreateClaim.ClaimCreatedSuccessfully(targetClaim, vcChunk)
                }
            }
        }
    }


    private fun getLastModifiedClaimForPlayer(
        context: VCPlayerContext
    ): VCClaim? {
        return context.claims.maxByOrNull { it.lastModified }
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
        confirmMerge: Boolean = false
    ): VCResult {
        val claim = context.claims.firstOrNull { it.displayName == oldName }
            ?: return VCResult.RenameClaim.OldNameNotFound(oldName)

        if(newName.length > context.restrictions.maxClaimNameLength && context.restrictions.maxClaimNameLength != -1) {
            return VCResult.RenameClaim.ClaimNameTooLong(context.restrictions.maxClaimNameLength)
        }

        val existingClaimWithNewName = context.claims.firstOrNull { it.displayName == newName }
        return if(existingClaimWithNewName != null){
            //a claim with the new name already exists
            if(!confirmMerge){
                VCResult.RenameClaim.ConfirmMergeRequired(
                    oldName = oldName,
                    newName = newName
                )
            }else{
                //merge claims
                updatePlayerContextCache(context) {
                    claimService.mergeClaims(
                        sourceClaim = claim,
                        targetClaim = existingClaimWithNewName,
                        player = context.player
                    )
                }
            }
        }else{
            //rename claim
            updatePlayerContextCache(context) {
                claimService.renameClaim(
                    claim = claim,
                    newName = newName,
                    player = context.player
                )
            }
        }
    }

    suspend fun renameForeignClaim(
        context: VCPlayerContext,
        targetPlayerName: String,
        oldName: String,
        newName: String,
        confirmMerge: Boolean = false
    ): VCResult {
        //check permission
        if(!context.restrictions.renameOtherPlayerClaims){
            return VCResult.MissingPermission
        }

        val claim = claimService.getClaimByNameAndPlayerName(
            claimName = oldName,
            playerName = targetPlayerName
        ) ?: return VCResult.RenameClaim.OldNameNotFound(oldName)

        if(newName.length > context.restrictions.maxClaimNameLength && context.restrictions.maxClaimNameLength != -1) {
            return VCResult.RenameClaim.ClaimNameTooLong(context.restrictions.maxClaimNameLength)
        }

        val owner = claimService.getOwnerOfClaim(claim) ?: return VCResult.UnknownFailure
        val claimsOfOwner = claimService.getPlayerContextByKey(owner.key)?.claims ?: return VCResult.UnknownFailure

        val existingClaimWithNewName = claimsOfOwner.firstOrNull { it.displayName == newName }
        return if(existingClaimWithNewName != null){
            //a claim with the new name already exists
            if(!confirmMerge){
                VCResult.RenameClaim.ConfirmMergeOtherPlayerClaimRequired(
                    oldName = oldName,
                    newName = newName,
                    playerName = targetPlayerName
                )
            }else{
                //merge claims
                updatePlayerContextCache(context) {
                    claimService.mergeClaims(
                        sourceClaim = claim,
                        targetClaim = existingClaimWithNewName,
                        player = owner
                    )
                }
            }
        }else{
            //rename claim
            updatePlayerContextCache(context) {
                claimService.renameClaim(
                    claim = claim,
                    newName = newName,
                    player = owner
                )
            }
        }
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

    suspend fun getDBPlayerContext(playerName: String): VCPlayerDBContext? {
        val player = claimService.getPlayerByName(playerName) ?: return null
        return claimService.getPlayerContextByKey(player.key)
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

    private suspend fun getFreshPlayerContextWithoutCaching(context: VCPlayerContext): VCPlayerContext? {
        val updatedContext = claimService.getPlayerContextByKey(context.player.key) ?: return null
        return VCPlayerContext(
            restrictions = context.restrictions,
            dbContext = updatedContext
        )
    }

    private suspend fun <T> updatePlayerContextCache(context: VCPlayerContext, exFirst: suspend () -> T): T  {
        val result = exFirst()
        val updatedContext = this.getFreshPlayerContextWithoutCaching(context)?: return result
        contextCache.put(updatedContext.player.mcUUID, updatedContext)
        return result
    }

    suspend fun getFreshPlayerContext(context: VCPlayerContext): VCPlayerContext?{
        val updatedContext = claimService.getPlayerContextByKey(context.player.key) ?: return null
        val newContext = VCPlayerContext(
            restrictions = context.restrictions,
            dbContext = updatedContext
        )
        contextCache.put(updatedContext.player.mcUUID, newContext)
        return newContext
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

    /**
     * Checks if a chunk can be claimed or is reserved
     */
    suspend fun isChunkReserved(chunk: PlainChunk): Boolean {
        //TODO: Implement check against WorldGuard Regions here!
        return false
    }

}