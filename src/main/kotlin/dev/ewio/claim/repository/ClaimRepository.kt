package dev.ewio.claim.repository

import dev.ewio.claim.definitions.VCClaim
import dev.ewio.database.VCClaims
import dev.ewio.database.rowToVCClaim
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class ClaimRepository {
    suspend fun create(claim: VCClaim) = newSuspendedTransaction(Dispatchers.IO) {
        VCClaims.insert {
            it[playerKey] = claim.playerKey
            it[displayName] = claim.displayName
            it[lastModified] = claim.lastModified
        }
        claim //TODO: return created claim with key
    }

    suspend fun findByKey(key: Int): VCClaim? = newSuspendedTransaction(Dispatchers.IO) {
        VCClaims.selectAll().where { VCClaims.key eq key }.firstOrNull()?.let(::rowToVCClaim)
    }

    suspend fun listByPlayer(playerKey: Int): List<VCClaim> = newSuspendedTransaction(Dispatchers.IO) {
        VCClaims.selectAll().where { VCClaims.playerKey eq playerKey }.map(::rowToVCClaim)
    }

    suspend fun deleteCascade(claimKey: Int) = newSuspendedTransaction(Dispatchers.IO) {
        // durch ON DELETE CASCADE werden die Chunks mit gelöscht
        VCClaims.deleteWhere { VCClaims.key eq claimKey }
    }

    suspend fun upsert(claim: VCClaim): VCClaim? = newSuspendedTransaction(Dispatchers.IO) {
        if(claim.key == -1) {
            // new claim
            val insertedId = VCClaims.insert {
                it[playerKey] = claim.playerKey
                it[displayName] = claim.displayName
                it[lastModified] = claim.lastModified
            } get VCClaims.key

            return@newSuspendedTransaction findByKey(insertedId)
        }else{
            //existing claim
            VCClaims.update({ VCClaims.key eq claim.key }) { st ->
                st[playerKey] = claim.playerKey
                st[displayName] = claim.displayName
                st[lastModified] = claim.lastModified
            }
            return@newSuspendedTransaction findByKey(claim.key)
        }
    }
}