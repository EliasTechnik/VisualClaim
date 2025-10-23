package dev.ewio.claim.repository.adapter

import dev.ewio.claim.ClaimService
import dev.ewio.claim.repository.adapter.exposed.ExposedChunkRepository
import dev.ewio.claim.repository.adapter.exposed.ExposedClaimRepository
import dev.ewio.claim.repository.adapter.exposed.ExposedPlayerRepository
import dev.ewio.claim.repository.definitions.VCChunk
import dev.ewio.claim.repository.definitions.VCClaim
import dev.ewio.claim.repository.definitions.VCPlayer
import dev.ewio.claim.repository.definitions.VCPlayer.Companion.dummy
import dev.ewio.claim.repository.interfaces.DBInterface
import dev.ewio.util.UKey
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

/**
 * A cache for transactions to optimize database operations and to ensure no key collisions occur during a transaction.
 */
class TransactionCache(
    val playerDB: ExposedPlayerRepository,
    val claimDB: ExposedClaimRepository,
    val chunkDB: ExposedChunkRepository,
) {

    fun claimTransaction(player: VCPlayer, env: (claims: List<VCClaim>, chunks: List<VCChunk>) -> VCClaim? ): VCClaim?{
        val cachedClaims = claimDB.findAll { it.playerKey == player.key }
        val cachedChunks = chunkDB.findAll { chunk ->
            cachedClaims.any { it.key == chunk.claimKey }
        }

        val claim = env( cachedClaims, cachedChunks)
        claim?.let{
            return claimDB.upsert(it)
        }
        return null
    }

    fun chunksTransaction(player: VCPlayer, env: (claims: List<VCClaim>, chunks: List<VCChunk>) -> List<VCChunk> ){
        val cachedClaims = claimDB.findAll { it.playerKey == player.key }
        val cachedChunks = chunkDB.findAll { chunk ->
            cachedClaims.any { it.key == chunk.claimKey }
        }

        val chunks = env(cachedClaims, cachedChunks)

        chunkDB.upsert(chunks)
    }

    fun chunksTransaction(claim: VCClaim, env: (chunks: List<VCChunk>) -> List<VCChunk> ){
        val cachedChunks = chunkDB.findAll { chunk ->
            claim.key== chunk.claimKey
        }

        val chunks = env(cachedChunks)

        chunkDB.upsert(chunks)
    }

    fun playerTransaction(player: VCPlayer, env: (player: VCPlayer) -> VCPlayer ){
        val updatedPlayer = env(player)
    }

    fun registerAndGetVCPlayer(uuid: UUID): VCPlayer {
        val player = playerDB.findAll {
            it.mcUUID == uuid
        }.firstOrNull()

        if(player != null){
            return player
        }else {
            val newPlayer = VCPlayer(
                key = UKey.Companion.dummy(),
                mcUUID = uuid,
                name = Bukkit.getPlayer(uuid)?.name ?: "Nameless",
                resolvedNameAt = System.currentTimeMillis()
            )
            return playerDB.upsert(newPlayer)
        }
    }
}