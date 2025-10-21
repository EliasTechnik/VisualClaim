package dev.ewio.claim

import dev.ewio.VisualClaim
import dev.ewio.claim.repository.interfaces.DBInterface
import dev.ewio.util.CMDStringWrapper
import dev.ewio.util.ChunkUKeyProvider
import dev.ewio.util.ClaimUKeyProvider
import dev.ewio.util.PlayerUKeyProvider
import dev.ewio.util.UKey
import dev.ewio.util.VCExceptionType
import dev.ewio.util.getMostSevereExceptionType
import org.bukkit.Bukkit
import java.util.UUID


class ClaimService(
    val claimRepository: DBInterface<VCClaim>,
    val chunkRepository: DBInterface<VCChunk>,
    val playerRepository: DBInterface<VCPlayer>,
    val plugin: VisualClaim,
    val partialMapUpdate: (changedClaim: VCClaim) -> Unit,
    val deleteFromMap: (deletedClaim: VCClaim) -> Unit
) {

    /**
     * Create a claim for a player with given chunks. If the claim with the given name already exists, it will be extended.
     * If the chunks of that claim are already claimed by another player they stay untouched.
     * Chunks already claimed by the same player will be transferred to the new claim.
     */
    fun createClaim(
        player: VCPlayer,
        chunks: List<PlainChunk>,
        useDefault: Boolean = true,
        name: String = plugin.cfg.get("default-claim-name")?.toString()?:"vc"
    ): Pair<VCExceptionType, VCClaim> {

        //register the player if not registered
        playerRepository.upsert(player)

        //get claim
        var claim = if (useDefault) {
            claimRepository.findAll {
                it.playerKey == player.key
            }.firstOrNull()
        } else {
            claimRepository.findAll {
                it.playerKey == player.key && it.displayName == name
            }.firstOrNull()
        }

        //if the claim does not exist, create it
        if(claim == null){
            claim = VCClaim(
                playerKey = player.key,
                key = ClaimUKeyProvider.nextKey(),
                displayName = name,
                isDefaultClaim = useDefault
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
                        val newChunk = dbChunk.copy(claimKey = claim.key)
                        chunkRepository.upsert(newChunk)
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
                key = ChunkUKeyProvider.nextKey(),
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
                chunkRepository.delete(it.key) { item -> item.copy(deleted = true) }
            }
            //delete the claim
            claimRepository.delete(claim.key) {item -> item.copy(deleted = true)}
            deleteFromMap(claim) //remove from map visualization
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

            chunkRepository.delete(dbChunk.key) {item -> item.copy(deleted = true)}
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
                key = PlayerUKeyProvider.nextKey(),
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

    fun getClaimAtChunk(chunk: PlainChunk):VCClaim? {
        val dbChunk = chunkRepository.findAll {
            it.plainChunk.toKey() == chunk.toKey()
        }.firstOrNull() ?: return null

        val claim = claimRepository.findAll {
            it.key == dbChunk.claimKey
        }.firstOrNull()
        return claim
    }

    fun purgeDB() {
        claimRepository.purge({it.deleted}, {it.key})
        chunkRepository.purge({it.deleted}, {it.key})
    }
}