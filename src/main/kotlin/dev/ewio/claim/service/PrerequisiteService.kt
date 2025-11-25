package dev.ewio.claim.service

import dev.ewio.annotations.Costly
import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCClaimDisplayData
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.claim.definitions.VCPlayerDBContext
import dev.ewio.claim.definitions.VCResult
import dev.ewio.util.SimpleCache
import dev.ewio.util.VCCache
import dev.ewio.util.error
import dev.ewio.util.log
import dev.ewio.util.logSevere
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID

class PrerequisiteService(
    private val claimService: ClaimService,
    private val permissionService: PermissionService,
    private val coroutineScope: CoroutineScope,
    private val cc: CentralCache,
    private val ui: UIService
) {

    private lateinit var movementService: MovementService

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

        //check if claim name is allowed
        if (!permissionService.isNameAllowed(claimName)) {
            return VCResult.CreateClaim.ClaimNameNotAllowed
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

                    return cc.updatePlayerContextCache(context.player.mcUUID) {
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
                cc.updatePlayerContextCache(context.player.mcUUID) {
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
            return when( cc.updatePlayerContextCache(context.player.mcUUID, {claimService.removeChunkFromClaim(vcChunk)})){
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
                return cc.updatePlayerContextCache(context.player.mcUUID, { claimService.removeChunkFromClaim(vcChunk) })
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
                cc.updatePlayerContextCache(context.player.mcUUID, {claimService.deleteClaim(claim)})
            }else{
                VCResult.DeleteClaim.NotOwnerOfClaim(claimName)
            }
        } else {
            //normal deletion
            val claim = context.claims.firstOrNull { it.displayName == claimName }
                ?: return VCResult.DeleteClaim.VCClaimNotFound(claimName)
            return cc.updatePlayerContextCache(context.player.mcUUID, {claimService.deleteClaim(claim)})

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

        if(!permissionService.isNameAllowed(newName)) {
            return VCResult.RenameClaim.ClaimNameNotAllowed
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
                cc.updatePlayerContextCache(context.player.mcUUID) {
                    claimService.mergeClaims(
                        sourceClaim = claim,
                        targetClaim = existingClaimWithNewName,
                        player = context.player
                    )
                }
            }
        }else{
            //rename claim
            cc.updatePlayerContextCache(context.player.mcUUID) {
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
                cc.updatePlayerContextCache(context.player.mcUUID) {
                    claimService.mergeClaims(
                        sourceClaim = claim,
                        targetClaim = existingClaimWithNewName,
                        player = owner
                    )
                }
            }
        }else{
            //rename claim
            cc.updatePlayerContextCache(context.player.mcUUID) {
                claimService.renameClaim(
                    claim = claim,
                    newName = newName,
                    player = owner
                )
            }
        }
    }

    suspend fun enableAutoclaim(
        context: VCPlayerContext,
        claimName: String
    ): VCResult {
        //log("Enabling autoclaim for player ${context.player.name} (${context.player.mcUUID}) to claim $claimName")
        if(!context.restrictions.canClaim){
            return VCResult.MissingPermission
        }

        //make sure the claim exists
        val claim = context.claims.firstOrNull { it.displayName == claimName }
            ?: return VCResult.AutoClaim.ClaimNeedsCreationFirst(claimName)

        cc.updatePlayerContextCache(context.player.mcUUID) {
            claimService.updatePlayer(
                player = context.player.copy(
                    autoClaim = true,
                    autoClaimTargetClaimKey = claim.key))
        }
        //log("Cached updated.")

        return VCResult.AutoClaim.AutoClaimEnabled(claim, movementService)
    }

    suspend fun disableAutoclaim(
        context: VCPlayerContext
    ): VCResult {
        //log("Disabling autoclaim for player ${context.player.name} (${context.player.mcUUID})")
        //disable autoclaim
        if(!context.restrictions.canClaim){
            return VCResult.MissingPermission
        }

        cc.updatePlayerContextCache(context.player.mcUUID) {
            claimService.updatePlayer(context.player.copy(autoClaim = false))
        }

        return VCResult.AutoClaim.AutoClaimDisabled
    }

    suspend fun handleAutoClaimOnMove(
        context: VCPlayerContext,
        chunk: PlainChunk
    ): VCResult{

        //check if autoclaim is configured right
        val claimToAppendTo = context.getAutoClaimTarget()?: run {
            return VCResult.AutoClaim.AutoClaimFailedNoTargetClaimSet
        }

        //check if the player can claim
        if (!context.restrictions.canClaim) return VCResult.MissingPermission

        //check if limit is reached
        if (context.chunks.size >= context.restrictions.maxChunks) {
            return VCResult.AutoClaim.ChunkLimitReached(context.restrictions.maxChunks)
        }

        //check if chunk is claimable (eg. not reserved)
        if(isChunkReserved(chunk)) return VCResult.AutoClaim.ChunkCanNotBeClaimed

        //check if chunk is free
        val existingClaim = claimService.getClaimAtChunk(chunk)
        if(existingClaim != null) {
            //the chunk is already part of a claim

            //check owner
            val owner = claimService.getOwnerOfClaim(existingClaim)
            if (owner == null) {
                //that is a db inconsistency
                error("Claim ($existingClaim) without valid owner!")
                return VCResult.UnknownFailure
            }

            if (owner.key != context.player.key) {
                //the chunk is owned by another player
                return VCResult.AutoClaim.ChunkClaimedByOtherPlayer(
                    otherPlayer = owner.name
                )
            }

            //check if the claim is the same as the target claim
            if (existingClaim.key == claimToAppendTo.key) {
                //the chunk is already part of the same claim
                return VCResult.AutoClaim.ChunkAlreadyClaimed
            } else {
                return VCResult.AutoClaim.ChunkBelongsToDifferentClaim(
                    otherClaimName = existingClaim.displayName,
                    chunk = chunk
                )
            }
        }else{
            //the chunk is free

            //append chunk to target claim
            val vcChunk = claimService.appendChunkToExistingClaim(
                chunk,
                claimToAppendTo,
                context.player
            )

            return if(vcChunk == null){
                VCResult.UnknownFailure
            }else {
                cc.updatePlayerContextCache(context.player.mcUUID) {
                    VCResult.AutoClaim.ChunkClaimed(claimToAppendTo, vcChunk)
                }
            }
        }
    }

    suspend fun updateBossbar(player: Player, context: VCPlayerContext, chunk: PlainChunk) {
        log("Updating bossbar for player ${player.name} (${player.uniqueId}) at chunk X:${chunk.x} Z:${chunk.z} in world ${chunk.world}")
        val claim = claimService.getClaimAtChunk(chunk)?.let { claimedChunk ->
            val owner = claimService.getPlayerByKey(claimedChunk.playerKey)
            if(owner != null){
                VCClaimDisplayData(
                    claim = claimedChunk,
                    ownerName = owner.name
                )
            }else{
                null
            }
        }
        ui.updateBossBar(player, claim, context)
    }

    suspend fun updateBossbar(playerUUID: UUID, chunk: PlainChunk){
        log("Updating bossbar for player UUID $playerUUID at chunk X:${chunk.x} Z:${chunk.z} in world ${chunk.world}")
       cc.getPlayerContext(playerUUID)?.let{ context ->
           Bukkit.getPlayer(playerUUID)?.let{ player ->
               log("Fetched player ${player.name} (${player.uniqueId}) for bossbar update.")
               updateBossbar(player, context, chunk)
           }
       }
    }

    /*
    suspend fun updateBossbarAfterClaim(player: Player, context: VCPlayerContext, claim: VCClaim?){
        if(claim == null) {
            ui.updateBossBar(player, null, context)
        }else{
            ui.updateBossBar(
                player,
                VCClaimDisplayData(
                    claim = claim,
                    ownerName = context.player.name
                ),
                context
            )
        }


    }

     */



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

    fun registerMovementService(service: MovementService) {
        movementService = service
    }

    /**
     * Wrapper to make this function available for command as they usually only have access to the prerequisiteService
     */
    suspend fun getPlayerContext(sender: CommandSender): Pair<VCPlayerContext, Player>? = cc.getPlayerContextFromSender(sender)

    fun getCachedPlayerContext(player: Player): VCPlayerContext? = cc.getCachedPlayerContext(player)

}