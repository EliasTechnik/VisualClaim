package dev.ewio.claim.repository

import dev.ewio.claim.definitions.VCLoadedChunk
import dev.ewio.claim.database.VCLoadedChunks
import dev.ewio.claim.database.VCPlayers
import dev.ewio.claim.database.rowToVCLoadedChunk
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class ChunkLoaderRepository {

    suspend fun findByChunkKey(chunkKey: String): VCLoadedChunk? = newSuspendedTransaction(Dispatchers.IO) {
        VCLoadedChunks.selectAll()
            .where { VCLoadedChunks.chunk eq chunkKey }
            .limit(1)
            .firstOrNull()
            ?.let(::rowToVCLoadedChunk)
    }

    suspend fun findByKey(key: Int): VCLoadedChunk? = newSuspendedTransaction(Dispatchers.IO) {
        VCLoadedChunks.selectAll().where { VCLoadedChunks.id eq key }.limit(1).firstOrNull()?.let(::rowToVCLoadedChunk)
    }

    suspend fun deleteByKey(key: Int) = newSuspendedTransaction(Dispatchers.IO) {
        VCLoadedChunks.deleteWhere { VCLoadedChunks.id eq key }
    }

    suspend fun listByPlayer(playerKey: Int): List<VCLoadedChunk> = newSuspendedTransaction(Dispatchers.IO) {
        (VCLoadedChunks innerJoin VCPlayers)
            .selectAll()
            .where { VCLoadedChunks.playerKey eq playerKey }
            .map(::rowToVCLoadedChunk)
    }

    suspend fun upsert(chunkLoader: VCLoadedChunk): VCLoadedChunk? = newSuspendedTransaction(Dispatchers.IO) {
        if(chunkLoader.key == -1) {
            // new chunkloader
            val insertedId = VCLoadedChunks.insertIgnoreAndGetId {
                it[playerKey] = chunkLoader.playerKey
                it[playerLocation] = chunkLoader.playerLocation.toKey()
                it[chunk] = chunkLoader.chunk.toKey()
                it[firstLoaded] = chunkLoader.firstLoaded
                it[name] = chunkLoader.name
            }

            return@newSuspendedTransaction VCLoadedChunks
                .selectAll()
                .where { VCLoadedChunks.id eq insertedId?.value }
                .limit(1)
                .firstOrNull()?.let(::rowToVCLoadedChunk)
        }else{
            // existing chunkLoader
            VCLoadedChunks.update({ VCLoadedChunks.id eq chunkLoader.key }) { st ->
                st[playerKey] = chunkLoader.playerKey
                st[playerLocation] = chunkLoader.playerLocation.toKey()
                st[chunk] = chunkLoader.chunk.toKey()
                st[firstLoaded] = chunkLoader.firstLoaded
                st[name] = chunkLoader.name
            }
            return@newSuspendedTransaction chunkLoader
        }
    }

    suspend fun all(): List<VCLoadedChunk> = newSuspendedTransaction {
        VCLoadedChunks.selectAll().let{ results ->
            results.map(::rowToVCLoadedChunk)
        }
    }
}
