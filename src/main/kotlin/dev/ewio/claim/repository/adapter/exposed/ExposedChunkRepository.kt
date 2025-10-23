package dev.ewio.claim.repository.adapter.exposed

import dev.ewio.claim.repository.definitions.VCChunk
import dev.ewio.claim.repository.interfaces.DBInterface
import dev.ewio.database.VCChunks
import dev.ewio.database.VCClaims
import dev.ewio.database.toVCChunk
import dev.ewio.util.UKey
import dev.ewio.util.chunkKey
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ExposedChunkRepository : DBInterface<VCChunk> {
    override fun find(key: UKey<VCChunk>): VCChunk? = transaction {
        VCChunks.selectAll().where { VCChunks.id eq key.value }.singleOrNull()?.toVCChunk()
    }

    override fun upsert(item: VCChunk): VCChunk = transaction {
        if (item.key.value < 0) {
            val id = VCChunks.insertAndGetId {
                it[claimKey] = org.jetbrains.exposed.dao.id.EntityID(item.claimKey.value, VCClaims)
                it[world] = item.plainChunk.world
                it[x] = item.plainChunk.x
                it[z] = item.plainChunk.z
            }.value
            item.copy(key = chunkKey(id))
        } else {
            VCChunks.update({ VCChunks.id eq item.key.value }) {
                it[claimKey] = org.jetbrains.exposed.dao.id.EntityID(item.claimKey.value, VCClaims)
                it[world] = item.plainChunk.world
                it[x] = item.plainChunk.x
                it[z] = item.plainChunk.z
            }
            item
        }
    }

    override fun delete(key: UKey<VCChunk>): Boolean = transaction {
        VCChunks.deleteWhere { VCChunks.id eq key.value } > 0
    }

    override fun all(): List<VCChunk> = transaction { VCChunks.selectAll().map { it.toVCChunk() } }
}