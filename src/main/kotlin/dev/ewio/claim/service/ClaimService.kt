package dev.ewio.claim.service

import dev.ewio.annotations.Lossy
import dev.ewio.claim.repository.ChunkRepository
import dev.ewio.claim.repository.ClaimRepository
import dev.ewio.claim.repository.PlayerRepository
import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCLoadedChunk
import dev.ewio.claim.definitions.VCPlayer
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.claim.definitions.VCPlayerDBContext
import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.repository.ChunkLoaderRepository
import dev.ewio.util.log
import org.bukkit.Bukkit
import java.util.UUID

class ClaimService(
    val playerRepo: PlayerRepository,
    val claimRepo: ClaimRepository,
    val chunkRepo: ChunkRepository,
    val chunkLoaderRepo: ChunkLoaderRepository,
    val placeOnMap: (player: VCPlayer, claim: VCClaim, chunks: List<VCChunk>) -> Unit,
    val deleteFromMap: (chunks: List<VCChunk>) -> Unit,
    val notifyOnUpdate: (chunks: List<PlainChunk>) -> Unit,
    val forceLoadChunk: (chunkLoader: VCLoadedChunk) -> Unit,
    val forceUnloadChunk: (chunkLoader: VCLoadedChunk) -> Unit
) {

    /**
     * Register a player by their UUID. If the player does not exist, they will be created.
     * Returns a VCPlayerContext containing the player, their claims, and their chunks.
     */

    suspend fun registerPlayerContextByUUID(uuid: UUID): VCPlayerDBContext? {
        var player = playerRepo.findByUUID(uuid)
        if(player == null){
            //register new player
            log("Registering new player with UUID $uuid")
            player = VCPlayer(
                mcUUID = uuid,
                name = Bukkit.getPlayer(uuid)?.name ?: "Nameless",
                resolvedNameAt = System.currentTimeMillis()
            )
            player = playerRepo.upsert(player)
        }

        if (player == null) {
            log("Failed to register or find player with UUID $uuid")
            return null
        }else{
            //get remaining data
            val claims = claimRepo.listByPlayer(player.key)
            val chunks = chunkRepo.listByPlayer(player.key)
            val chunkLoaders = chunkLoaderRepo.listByPlayer(player.key)

            log("Loaded player context for player ${player.name} (UUID: ${player.mcUUID}), Claims: ${claims.size}, Chunks: ${chunks.size}")

            return VCPlayerDBContext(
                player = player,
                claims = claims,
                chunks = chunks,
                chunkLoader = chunkLoaders
            )
        }
    }

    suspend fun getPlayerContextByKey(key: Int): VCPlayerDBContext? {
        val player = playerRepo.findByKey(key)

        if (player == null) {
            return null
        }else{
            //get remaining data
            val claims = claimRepo.listByPlayer(player.key)
            val chunks = chunkRepo.listByPlayer(player.key)
            val chunkLoaders = chunkLoaderRepo.listByPlayer(player.key)

            return VCPlayerDBContext(
                player = player,
                claims = claims,
                chunks = chunks,
                chunkLoader = chunkLoaders
            )
        }
    }

    suspend fun createEmptyClaim(
        player: VCPlayer,
        claimName: String
    ): VCClaim? {
        val claim = claimRepo.upsert(
            VCClaim(
                playerKey = player.key,
                displayName = claimName
            )
        )

        claim?.let{
            log("Created new claim '${it.displayName}' (key: ${it.key}) for player ${player.name} (UUID: ${player.mcUUID})")
        }?: log("Failed to create new claim '$claimName' for player ${player.name} (UUID: ${player.mcUUID}). The Database returned null.")

        return claim

    }

    suspend fun transferChunkToAnotherClaim(
        chunk: VCChunk,
        targetClaim: VCClaim,
        player: VCPlayer
    ): VCResult {
        //find the VCChunk
        val dbChunk = chunkRepo.findByKey(chunk.key)

        if(dbChunk == null) {
            return VCResult.TransferChunk.VCChunkNotFound
        } else {
            //transfer

            //first remove from map
            deleteFromMap(listOf(dbChunk))

            val newChunk = dbChunk.copy(
                claimKey = targetClaim.key
            )
            chunkRepo.upsert(newChunk)
            placeOnMap(player,targetClaim, listOf(newChunk))
            notifyOnUpdate(listOf(newChunk.plainChunk))
            return VCResult.TransferChunk.TransferSuccessful
        }
    }

    suspend fun getVCChunkByPlainChunk(chunk: PlainChunk): VCChunk? {
        return chunkRepo.findByWorldXZ(chunk.world, chunk.x, chunk.z)
    }

    suspend fun appendChunkToExistingClaim(
        chunk: PlainChunk,
        claim: VCClaim,
        player: VCPlayer
    ): VCChunk? {
        val vcChunk = chunkRepo.upsert(
            VCChunk(
                claimKey = claim.key,
                plainChunk = chunk
            )
        )
        vcChunk?.let{
            placeOnMap(player,claim, listOf(it))
            log("Notifying on update for chunk ${it.plainChunk.world}:${it.plainChunk.x}:${it.plainChunk.z}")
            notifyOnUpdate(listOf(it.plainChunk))
        }
        return vcChunk
    }

    suspend fun getVCChunkOwner(chunk: VCChunk): VCPlayer? {
        val dbChunk = chunkRepo.findByKey(chunk.key) ?: return null
        val claim = claimRepo.findByKey(dbChunk.claimKey) ?: return null
        return playerRepo.findByKey(claim.playerKey)
    }

    suspend fun removeChunkFromClaim(
        chunk: VCChunk
    ): VCResult {
        val dbChunk = chunkRepo.findByKey(chunk.key)
        if(dbChunk == null) {
            return VCResult.UnclaimChunk.UnclaimAlreadyUnclaimed
        } else {
            chunkRepo.deleteByKey(dbChunk.key)
            deleteFromMap(listOf(dbChunk)) //remove from map visualization
            notifyOnUpdate(listOf(dbChunk.plainChunk))
            return VCResult.UnclaimChunk.UnclaimSuccessful("")
        }
    }

    suspend fun deleteClaim(claim: VCClaim): VCResult {
        //remove from map
        val chunks = chunkRepo.listByClaim(claim.key)
        deleteFromMap(chunks)
        notifyOnUpdate(chunks.map { it.plainChunk })

        //delete all chunks of the claim
        claimRepo.deleteCascade(claim.key)
        return VCResult.DeleteClaim.RemovedSuccessful(claim.displayName)
    }

    suspend fun renameClaim(claim: VCClaim, newName: String, player: VCPlayer): VCResult {
        claimRepo.findByKey(claim.key)?.let{
            //remove from map
            val chunks = chunkRepo.listByClaim(claim.key)
            deleteFromMap(chunks)

            val renamedClaim = it.copy(displayName = newName)
            val updatedClaim = claimRepo.upsert(renamedClaim)

            return if(updatedClaim == null) {
                VCResult.UnknownFailure
            }else{
                placeOnMap(player,updatedClaim, chunks) //re-add to map
                notifyOnUpdate(chunks.map { it.plainChunk })
                VCResult.RenameClaim.RenamedSuccessful(claim.displayName, updatedClaim.displayName)
            }
        }
        return VCResult.RenameClaim.OldNameNotFound(claim.displayName)
    }

    suspend fun mergeClaims(
        sourceClaim: VCClaim,
        targetClaim: VCClaim,
        player: VCPlayer
    ): VCResult {
        //get all chunks of source claim
        val sourceChunks = chunkRepo.listByClaim(sourceClaim.key)
        val targetChunks = chunkRepo.listByClaim(targetClaim.key)

        //remove both claims from map
        deleteFromMap(sourceChunks)
        deleteFromMap(targetChunks)

        //reassign all chunks from source to target
        sourceChunks.forEach { chunk ->
            val updatedChunk = chunk.copy(
                claimKey = targetClaim.key
            )
            chunkRepo.upsert(updatedChunk)
        }

        //delete source claim
        claimRepo.deleteCascade(sourceClaim.key)

        //re-add target claim to map
        val allTargetChunks = chunkRepo.listByClaim(targetClaim.key)
        placeOnMap(player,targetClaim, allTargetChunks)
        notifyOnUpdate(allTargetChunks.map { it.plainChunk })

        return VCResult.RenameClaim.MergeSuccessful(sourceClaim.displayName, targetClaim.displayName)
    }

    suspend fun placeAllClaimsOnMap(){
        val allPlayers = playerRepo.all()
        val allClaims = claimRepo.all()
        val allChunks = chunkRepo.all()

        allPlayers.forEach { player ->
            val playerClaims = allClaims.filter { it.playerKey == player.key }
            playerClaims.forEach { claim ->
                val claimChunks = allChunks.filter { it.claimKey == claim.key }
                placeOnMap(player, claim, claimChunks)
            }
        }
    }

    suspend fun deleteAllClaimsFromMap(){
        val allChunks = chunkRepo.all()
        deleteFromMap(allChunks)
    }

    suspend fun getAllPlayers(): List<VCPlayer> {
        return playerRepo.all()
    }

    suspend fun getClaimAtChunk(chunk: PlainChunk): VCClaim? {
        val vcChunk = chunkRepo.findByWorldXZ(
            world = chunk.world,
            x = chunk.x,
            z = chunk.z
        ) ?: return null

        return claimRepo.findByKey(vcChunk.claimKey)
    }

    suspend fun getPlayerByKey(key: Int): VCPlayer? {
        return playerRepo.findByKey(key)
    }

    suspend fun getOwnerOfChunk(vcChunk: VCChunk): VCPlayer? {
        val claim = claimRepo.findByKey(vcChunk.claimKey) ?: return null

        return playerRepo.findByKey(claim.playerKey)
    }

    @Lossy
    suspend fun getClaimByNameAndPlayerName(claimName: String, playerName: String): VCClaim? {
        val player = playerRepo.findByName(playerName).maxByOrNull { it.resolvedNameAt } //this will get the most recently resolved name (if there is a collision)
        // this might be not ideal since names can change.
        // There should be only one player with the exact name at any given time, but because of how the VC database works collisions are possible.
        // Because this is a hard and rare edge case, we will ignore it for now and only take the first match. This might lead to
        // claims being undeletable by admins. When this happens we need to implement a better way to delete claims of other players.

        player?.let {
            return claimRepo.listByPlayer(it.key).filter { claim -> claim.displayName == claimName }.maxByOrNull { it.lastModified }
        }

        return null
    }

    @Lossy
    suspend fun getPlayerByName(playerName: String): VCPlayer? {
        return playerRepo.findByName(playerName).maxByOrNull { it.resolvedNameAt }
    }

    suspend fun getOwnerOfClaim(claim: VCClaim): VCPlayer? {
        return getPlayerByKey(claim.playerKey)
    }

    suspend fun updatePlayer(player: VCPlayer): VCPlayer? {
        return playerRepo.upsert(player)
    }

    suspend fun removeTriggerWordsFromClaims(triggerWords: List<String>, migrationSuffix: String): Int {
        val allClaims = claimRepo.all()
        var modifiedCount = 0

        allClaims.forEach { claim ->
            triggerWords.filterNot{it == migrationSuffix}.forEach { triggerWord ->
                if (claim.displayName.equals(triggerWord, ignoreCase = true)) {
                    val renamedClaim = claim.copy(displayName = migrationSuffix)
                    claimRepo.upsert(renamedClaim)
                    modifiedCount++
                    log("Renamed claim ${claim.key} '${claim.displayName}' to '${renamedClaim.displayName}' to remove trigger words.")
                }
            }
        }

        return modifiedCount
    }

    suspend fun getVCLoadedChunksForPlayer(player: VCPlayer): List<VCLoadedChunk> {
        return chunkLoaderRepo.listByPlayer(player.key)
    }

    suspend fun getVCChunkLoaderByChunk(chunk: PlainChunk): VCLoadedChunk? {
        return chunkLoaderRepo.findByChunkKey(chunk.toKey())
    }

    suspend fun addVCLoadedChunk(chunkLoader: VCLoadedChunk): VCLoadedChunk? {
        val cl = chunkLoaderRepo.upsert(chunkLoader)
        if(cl != null){
            forceLoadChunk(cl)
            notifyOnUpdate(listOf(cl.chunk))
        }
        return cl
    }

    suspend fun removeVCLoadedChunkByKey(key: Int) {
        val cl = chunkLoaderRepo.findByKey(key)
        if(cl != null){
            forceUnloadChunk(cl)
            notifyOnUpdate(listOf(cl.chunk))
        }
        chunkLoaderRepo.deleteByKey(key)
    }

    suspend fun loadAllChunkLoaders(){
        val allChunkLoaders = chunkLoaderRepo.all()
        allChunkLoaders.forEach { cl ->
            forceLoadChunk(cl)
        }
    }

    suspend fun unloadAllChunkLoaders(){
        val allChunkLoaders = chunkLoaderRepo.all()
        allChunkLoaders.forEach { cl ->
            forceUnloadChunk(cl)
        }
    }

}