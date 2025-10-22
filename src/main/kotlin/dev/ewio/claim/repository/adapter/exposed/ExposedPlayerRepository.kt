package dev.ewio.claim.repository.adapter.exposed

import dev.ewio.claim.repository.definitions.VCPlayer
import dev.ewio.claim.repository.interfaces.DBInterface
import dev.ewio.database.VCPlayers
import dev.ewio.database.toVCPlayer
import dev.ewio.util.UKey
import dev.ewio.util.playerKey
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ExposedPlayerRepository : DBInterface<VCPlayer> {
    override fun find(key: UKey<VCPlayer>): VCPlayer? = transaction {
        VCPlayers.select { VCPlayers.id eq key.value }.singleOrNull()?.toVCPlayer()
    }

    override fun upsert(item: VCPlayer): VCPlayer = transaction {
        if (item.key.value < 0) {
            val id = VCPlayers.insertAndGetId {
                it[mcUuid] = item.mcUUID.toString()
                it[name] = item.name
                it[resolvedNameAt] = item.resolvedNameAt
                it[autoClaim] = item.autoClaim
            }.value
            item.copy(key = playerKey(id))
        } else {
            VCPlayers.update({ VCPlayers.id eq item.key.value }) {
                it[mcUuid] = item.mcUUID.toString()
                it[name] = item.name
                it[resolvedNameAt] = item.resolvedNameAt
                it[autoClaim] = item.autoClaim
            }
            item
        }
    }

    override fun delete(key: UKey<VCPlayer>): Boolean = transaction {
        VCPlayers.deleteWhere { VCPlayers.id eq key.value } > 0
    }

    override fun all(): List<VCPlayer> = transaction { VCPlayers.selectAll().map { it.toVCPlayer() } }
}