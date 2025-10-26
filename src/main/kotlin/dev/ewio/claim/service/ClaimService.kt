package dev.ewio.claim.service

import dev.ewio.claim.repository.ChunkRepository
import dev.ewio.claim.repository.ClaimRepository
import dev.ewio.claim.repository.PlayerRepository
import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCPlayer
import dev.ewio.claim.definitions.VCPlayerDBContext
import dev.ewio.claim.definitions.VCResult
import org.bukkit.Bukkit
import java.util.UUID

class ClaimService(
    val playerRepo: PlayerRepository,
    val claimRepo: ClaimRepository,
    val chunkRepo: ChunkRepository,
    val placeOnMap: (player: VCPlayer, claim: VCClaim, chunks: List<VCChunk>) -> Unit,
    val deleteFromMap: (chunks: List<VCChunk>) -> Unit
) {

    /**
     * Register a player by their UUID. If the player does not exist, they will be created.
     * Returns a VCPlayerContext containing the player, their claims, and their chunks.
     */

    suspend fun registerPlayerContextByUUID(uuid: UUID): VCPlayerDBContext? {
        var player = playerRepo.findByUUID(uuid)
        if(player == null){
            //register new player
            player = VCPlayer(
                mcUUID = uuid,
                name = Bukkit.getPlayer(uuid)?.name ?: "Nameless",
                resolvedNameAt = System.currentTimeMillis()
            )
            player = playerRepo.upsert(player)
        }

        if (player == null) {
            return null
        }else{
            //get remaining data
            val claims = claimRepo.listByPlayer(player.key)
            val chunks = chunkRepo.listByPlayer(player.key)

            return VCPlayerDBContext(
                player = player,
                claims = claims,
                chunks = chunks
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

            return VCPlayerDBContext(
                player = player,
                claims = claims,
                chunks = chunks
            )
        }
    }

    suspend fun createEmptyClaim(
        player: VCPlayer,
        claimName: String
    ): VCClaim {
        return claimRepo.create(
            VCClaim(
                playerKey = player.key,
                displayName = claimName
            )
        )
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
            return VCResult.RemoveChunk.VCChunkNotFound
        } else {
            chunkRepo.deleteByKey(dbChunk.key)
            deleteFromMap(listOf(dbChunk)) //remove from map visualization
            return VCResult.RemoveChunk.RemovedSuccessful
        }
    }

    suspend fun deleteClaim(claim: VCClaim): VCResult {
        //remove from map
        val chunks = chunkRepo.listByClaim(claim.key)
        deleteFromMap(chunks)

        //delete all chunks of the claim
        claimRepo.deleteCascade(claim.key)
        return VCResult.DeleteClaim.RemovedSuccessful
    }

    suspend fun renameClaim(claim: VCClaim, newName: String, player: VCPlayer): VCResult {
        claimRepo.findByKey(claim.key)?.let{
            //remove from map
            val chunks = chunkRepo.listByClaim(claim.key)
            deleteFromMap(chunks)

            val renamedClaim = it.copy(displayName = newName)
            val updatedClaim = claimRepo.upsert(renamedClaim)

            updatedClaim?.let { uc ->
                placeOnMap(player,uc, chunks) //re-add to map
            }

            return VCResult.RenameClaim.RenamedSuccessful
        }
        return VCResult.RenameClaim.VCClaimNotFound
    }
}