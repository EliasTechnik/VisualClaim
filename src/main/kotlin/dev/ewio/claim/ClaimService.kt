package dev.ewio.claim

import dev.ewio.VisualClaim
import dev.ewio.claim.repository.definitions.PlainChunk
import dev.ewio.claim.repository.definitions.VCChunk
import dev.ewio.claim.repository.definitions.VCClaim
import dev.ewio.claim.repository.definitions.VCPlayer
import dev.ewio.claim.repository.interfaces.DBInterface
import dev.ewio.util.UKey
import dev.ewio.util.UKey.Companion.dummy
import dev.ewio.util.VCExceptionType
import dev.ewio.util.VCRenameResultType
import dev.ewio.util.getMostSevereExceptionType
import org.bukkit.Bukkit
import java.util.UUID


class ClaimService(
    val claimRepository: DBInterface<VCClaim>,
    val chunkRepository: DBInterface<VCChunk>,
    val playerRepository: DBInterface<VCPlayer>,
    val plugin: VisualClaim,
    val partialMapUpdate: (changedClaim: VCClaim) -> Unit,
    val deleteFromMap: (chunks: List<VCChunk>) -> Unit
) {

    /**
     * Create a claim for a player with given chunks. If the claim with the given name already exists, it will be extended.
     * If the chunks of that claim are already claimed by another player they stay untouched.
     * Chunks already claimed by the same player will be transferred to the new claim.
     */
    fun createClaim(
        player: VCPlayer,
        chunks: List<PlainChunk>,
        name: String = ""
    ): Pair<VCExceptionType, VCClaim> {

        //register the player if not registered
        playerRepository.upsert(player)

        val appendToLast = name == ""

        //get claim
        var claim = if (appendToLast) {
            claimRepository.findAll {
                it.playerKey == player.key
            }.maxByOrNull { it.lastModified }
        } else {
            claimRepository.findAll {
                it.playerKey == player.key && it.displayName == name
            }.firstOrNull()
        }

        if(claim == null && appendToLast){
            //no last claim found where we can append to
            return Pair(VCExceptionType.NO_CLAIM_FOUND, VCClaim.dummy())
        }

        //if the claim does not exist, create it
        if(claim == null){
            claim = VCClaim(
                playerKey = player.key,
                key = dummy(), //temporary dummy key, will be replaced on upsert
                displayName = name,
            )
            claimRepository.upsert(claim)
        }

        //add chunks to the claim
        val extendResult = extendClaim(claim, chunks)
        if(extendResult == VCExceptionType.NONE){
            //successful claim creation or extension
            partialMapUpdate(claim) //update map visualization
        }
        return Pair(extendResult, claim)
    }

    //extend claim with one or multiple chunks
    private fun extendClaim(claim: VCClaim, chunks: List<PlainChunk>): VCExceptionType {
        val eList = mutableListOf<VCExceptionType>()
        chunks.forEach { chunk ->
            eList.add(extendClaim(claim, chunk))
        }
        return getMostSevereExceptionType(eList)
    }

    private fun extendClaim(claim: VCClaim, chunks: List<VCChunk>){
        val plainChunks = chunks.map { it.plainChunk }
        extendClaim(claim, plainChunks)
    }

    //extend claim with one chunk
    private fun extendClaim(claim: VCClaim, chunk: PlainChunk): VCExceptionType{
        val existingClaim = getClaimForChunk(chunk)

        if(existingClaim != null){
            //this chunk is claimed but we might transfer it
            if(existingClaim.key == claim.key){
                //this chunk is allready claimed by the same claim
                return VCExceptionType.CHUNK_ALREADY_CLAIMED_BY_SAME_CLAIM
            }

            val owner = getChunkOwner(chunk)
            if(owner != null){
                if(owner.key == claim.playerKey){
                    //this chunk is already claimed by the same player, transfer it

                    val dbChunk = getVCChunkFromClaim(existingClaim, chunk)
                    if (dbChunk != null) {
                        deleteFromMap(listOf(dbChunk)) //remove from map visualization
                        val newChunk = dbChunk.copy(claimKey = claim.key)
                        chunkRepository.upsert(newChunk)
                        partialMapUpdate(claim) //update map visualization
                        return VCExceptionType.NONE
                    }else{
                        return VCExceptionType.VCCHUNK_NOT_FOUND //this should never happen
                    }
                } else {
                    return VCExceptionType.CHUNK_CLAIMED_BY_OTHER_PLAYER
                }
            }else{
                return VCExceptionType.OWNER_OF_CLAIMED_CHUNK_NOT_FOUND
            }
        }else{
            //this chunk is not claimed yet, claim it

            val newChunk = VCChunk(
                key = dummy(),
                claimKey = claim.key,
                plainChunk = chunk
            )
            chunkRepository.upsert(newChunk)
            return VCExceptionType.NONE
        }
    }

    fun getVCChunkFromClaim(claim: VCClaim, chunk: PlainChunk): VCChunk?{
        return chunkRepository.findAll {
            it.plainChunk.toKey() == chunk.toKey() && it.claimKey == claim.key
        }.firstOrNull()
    }

    fun getClaimForChunk(chunk: PlainChunk): VCClaim?{
        val dbChunk = chunkRepository.findAll {
            it.plainChunk.toKey() == chunk.toKey()
        }.firstOrNull() ?: return null

        val claim = claimRepository.findAll {
            it.key == dbChunk.claimKey
        }.firstOrNull()
        //If a chunk exists without a claim, we have a data integrity issue
        if(claim == null){
            plugin.logger.warning("Data integrity issue: Chunk ${chunk.toKey()} exists without a valid claim")
        }
        return claim
    }

    fun getChunkOwner(chunk: PlainChunk): VCPlayer?{
        val dbChunk = chunkRepository.findAll {
            it.plainChunk.toKey() == chunk.toKey()
        }.firstOrNull() ?: return null

        val claim = claimRepository.findAll {
            it.key == dbChunk.claimKey
        }.firstOrNull() ?: return null

        return playerRepository.findAll {
            it.key == claim.playerKey
        }.firstOrNull()
    }

    fun deleteClaim(player: VCPlayer, claim: VCClaim): VCExceptionType {
        //check if the claim belongs to the player
        if(claim.playerKey != player.key) {
            return VCExceptionType.CLAIM_BELONGS_TO_OTHER_PLAYER
        }else{
            //delete all chunks of the claim
            val chunks = chunkRepository.findAll {
                it.claimKey == claim.key
            }

            chunks.forEach {
                chunkRepository.delete(it.key)
            }
            //delete the claim
            claimRepository.delete(claim.key)
            deleteFromMap(chunks) //remove from map visualization
            return VCExceptionType.NONE
        }
    }

    fun removeChunkFromClaim(player: VCPlayer, claim: VCClaim, chunk: PlainChunk): VCExceptionType {
        //check if the claim belongs to the player
        if(claim.playerKey != player.key) {
            return VCExceptionType.CHUNK_CLAIMED_BY_OTHER_PLAYER
        }else{
            val dbChunk = chunkRepository.findAll {
                it.plainChunk.toKey() == chunk.toKey() && it.claimKey == claim.key
            }.firstOrNull() ?: return VCExceptionType.VCCHUNK_NOT_FOUND

            chunkRepository.delete(dbChunk.key)
            deleteFromMap(listOf(dbChunk)) //remove from map visualization
            partialMapUpdate(claim) //update map visualization
            return VCExceptionType.NONE
        }
    }

    fun registerAndGetVCPlayerByUUID(uuid: UUID): VCPlayer {
        val player = playerRepository.findAll {
            it.mcUUID == uuid
        }.firstOrNull()

        if(player != null){
            return player
        }else {
            val newPlayer = VCPlayer(
                key = dummy(),
                mcUUID = uuid,
                name = Bukkit.getPlayer(uuid)?.name ?: "Nameless",
                resolvedNameAt = System.currentTimeMillis()
            )
            playerRepository.upsert(newPlayer)
            return newPlayer
        }
    }

    fun getClaimsOfPlayer(player: VCPlayer): List<VCClaim> {
        return claimRepository.findAll {
            it.playerKey == player.key
        }
    }

    fun getChunksOfClaim(claim: VCClaim): List<VCChunk> {
        return chunkRepository.findAll {
            it.claimKey == claim.key
        }
    }

    fun getPlayerByKey(key: UKey<VCPlayer>): VCPlayer? {
        return playerRepository.findAll {
            it.key == key
        }.firstOrNull()
    }

    fun getChunkCountOfPlayer(player: VCPlayer): Int {
        val claims = getClaimsOfPlayer(player)
        var count = 0
        claims.forEach { claim ->
            val chunks = getChunksOfClaim(claim)
            count += chunks.size
        }
        return count
    }

    fun getClaimAtChunk(chunk: PlainChunk): VCClaim? {
        val dbChunk = chunkRepository.findAll {
            it.plainChunk.toKey() == chunk.toKey()
        }.firstOrNull() ?: return null

        val claim = claimRepository.findAll {
            it.key == dbChunk.claimKey
        }.firstOrNull()
        return claim
    }


    fun renameAndMergeClaim(player: VCPlayer, oldName: String, newName: String, merge: Boolean = false): Triple<VCRenameResultType, String, String> {
        //get claims of player
        val claims = getClaimsOfPlayer(player)

        //get claim
        val claim = claims.firstOrNull {
            it.displayName == oldName
        } ?: return Triple(VCRenameResultType.OLD_CLAIM_NOT_FOUND, oldName, newName)

        //check if new name already exists
        val existingClaim = claims.firstOrNull {
            it.displayName == newName
        }

        if(existingClaim != null){
            if(existingClaim.key == claim.key){
                //same claim, user is idiot
                return Triple(VCRenameResultType.SUCCESS, oldName, newName)
            }

            if(merge){
                //merge claims
                val chunksToTransfer = getChunksOfClaim(claim)

                //remove old claim from map visualization
                deleteFromMap(chunksToTransfer)

                //delete old VCChunks
                chunksToTransfer.forEach {
                    chunkRepository.delete(it.key)
                }

                //delete old claim
                claimRepository.delete(claim.key)

                //transfer chunks
                extendClaim(existingClaim, chunksToTransfer)

                //update last modified
                claimRepository.upsert(existingClaim.copy(lastModified = System.currentTimeMillis()))

                //update map
                partialMapUpdate(existingClaim) //update map visualization
                return Triple(VCRenameResultType.MERGED, oldName, newName)
            }else {
                return Triple(VCRenameResultType.NAME_ALREADY_EXISTS, oldName, newName)
            }
        }else{
            //there is no existing claim with the new name, just rename
            //remove old claim from map visualization
            val chunks = getChunksOfClaim(claim)
            deleteFromMap(chunks)
            //rename
            val renamedClaim = claim.copy(displayName = newName, lastModified = System.currentTimeMillis())
            claimRepository.upsert(renamedClaim)
            partialMapUpdate(renamedClaim) //update map visualization //se if this works
            return Triple(VCRenameResultType.SUCCESS, oldName, newName)
        }
    }
}