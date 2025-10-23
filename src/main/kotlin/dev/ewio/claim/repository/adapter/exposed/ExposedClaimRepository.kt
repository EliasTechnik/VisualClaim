package dev.ewio.claim.repository.adapter.exposed

import dev.ewio.claim.repository.definitions.VCClaim
import dev.ewio.claim.repository.interfaces.DBInterface
import dev.ewio.database.VCClaims
import dev.ewio.database.VCPlayers
import dev.ewio.database.toVCClaim
import dev.ewio.util.UKey
import dev.ewio.util.claimKey
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ExposedClaimRepository : DBInterface<VCClaim> {
    override fun find(key: UKey<VCClaim>): VCClaim? = transaction {
        VCClaims.selectAll().where { VCClaims.id eq key.value }.singleOrNull()?.toVCClaim()
    }

    override fun upsert(item: VCClaim): VCClaim = transaction {
        if (item.key.value < 0) {
            val id = VCClaims.insertAndGetId {
                it[playerKey] = org.jetbrains.exposed.dao.id.EntityID(item.playerKey.value, VCPlayers)
                it[displayName] = item.displayName
                it[lastModified] = item.lastModified
            }.value
            item.copy(key = claimKey(id))
        } else {
            VCClaims.update({ VCClaims.id eq item.key.value }) {
                it[playerKey] = org.jetbrains.exposed.dao.id.EntityID(item.playerKey.value, VCPlayers)
                it[displayName] = item.displayName
                it[lastModified] = item.lastModified
            }
            item
        }
    }

    override fun delete(key: UKey<VCClaim>): Boolean = transaction {
        VCClaims.deleteWhere { VCClaims.id eq key.value } > 0
    }

    override fun all(): List<VCClaim> = transaction { VCClaims.selectAll().map { it.toVCClaim() } }
}