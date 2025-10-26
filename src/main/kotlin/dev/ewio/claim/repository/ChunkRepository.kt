package dev.ewio.claim.repository

import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCPlayer
import dev.ewio.database.VCChunks
import dev.ewio.database.VCClaims
import dev.ewio.database.VCPlayers
import dev.ewio.database.rowToVCChunk
import dev.ewio.database.rowToVCPlayer
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class ChunkRepository {
    /*
    suspend fun create(chunk: VCChunk) = newSuspendedTransaction(Dispatchers.IO) {
        VCChunks.insert {
            it[claimKey] = chunk.claimKey // muss existieren -> sonst FK-Fehler
            it[world] = chunk.plainChunk.world
            it[x] = chunk.plainChunk.x
            it[z] = chunk.plainChunk.z
        }
        //TODO: return created chunk with key
        chunk
    }

     */

    suspend fun findByWorldXZ(world: String, x: Int, z: Int): VCChunk? = newSuspendedTransaction(Dispatchers.IO) {
        VCChunks.selectAll()
            .where { (VCChunks.world eq world) and (VCChunks.x eq x) and (VCChunks.z eq z) }
            .limit(1)
            .firstOrNull()
            ?.let(::rowToVCChunk)
    }

    suspend fun findByKey(key: Int): VCChunk? = newSuspendedTransaction(Dispatchers.IO) {
        VCChunks.selectAll().where { VCChunks.id eq key }.limit(1).firstOrNull()?.let(::rowToVCChunk)
    }

    suspend fun listByClaim(claimKey: Int): List<VCChunk> = newSuspendedTransaction(Dispatchers.IO) {
        VCChunks.selectAll().where { VCChunks.claimKey eq claimKey }.map(::rowToVCChunk)
    }

    suspend fun deleteByKey(key: Int) = newSuspendedTransaction(Dispatchers.IO) {
        VCChunks.deleteWhere { VCChunks.id eq key }
    }

    suspend fun listByPlayer(playerKey: Int): List<VCChunk> = newSuspendedTransaction(Dispatchers.IO) {
        (VCChunks innerJoin VCClaims)
            .selectAll()
            .where { VCClaims.playerKey eq playerKey }
            .map(::rowToVCChunk)
    }

    suspend fun upsert(chunk: VCChunk): VCChunk? = newSuspendedTransaction(Dispatchers.IO) {
        if(chunk.key == -1) {
            // new chunk
            val insertedId = VCChunks.insertIgnoreAndGetId {
                it[claimKey] = chunk.claimKey
                it[world] = chunk.plainChunk.world
                it[x] = chunk.plainChunk.x
                it[z] = chunk.plainChunk.z
            }

            return@newSuspendedTransaction VCChunks
                .selectAll()
                .where { VCChunks.id eq insertedId?.value }
                .limit(1)
                .firstOrNull()?.let(::rowToVCChunk)
        }else{
            // existing chunk
            VCChunks.update({ VCChunks.id eq chunk.key }) { st ->
                st[claimKey] = chunk.claimKey
                st[world] = chunk.plainChunk.world
                st[x] = chunk.plainChunk.x
                st[z] = chunk.plainChunk.z
            }
            return@newSuspendedTransaction chunk
        }
    }

    suspend fun all(): List<VCChunk> = newSuspendedTransaction {
        VCChunks.selectAll().let{ results ->
            results.map(::rowToVCChunk)
        }
    }

}