package dev.ewio.claim.repository

import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCPlayer
import dev.ewio.database.VCClaims
import dev.ewio.database.VCPlayers
import dev.ewio.database.rowToVCClaim
import dev.ewio.database.rowToVCPlayer
import dev.ewio.util.log
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class ClaimRepository {
    /*suspend fun create(claim: VCClaim) = newSuspendedTransaction(Dispatchers.IO) {
        VCClaims.insert {
            it[playerKey] = claim.playerKey
            it[displayName] = claim.displayName
            it[lastModified] = claim.lastModified
        }
        claim //TODO: return created claim with key
    }

     */

    suspend fun findByKey(key: Int): VCClaim? = newSuspendedTransaction(Dispatchers.IO) {
        VCClaims.selectAll().where { VCClaims.id eq key }.limit(1).firstOrNull()?.let(::rowToVCClaim)
    }

    suspend fun listByPlayer(playerKey: Int): List<VCClaim> = newSuspendedTransaction(Dispatchers.IO) {
        VCClaims.selectAll().where { VCClaims.playerKey eq playerKey }.map(::rowToVCClaim)
    }

    suspend fun deleteCascade(claimKey: Int) = newSuspendedTransaction(Dispatchers.IO) {
        // durch ON DELETE CASCADE werden die Chunks mit gelöscht
        VCClaims.deleteWhere { VCClaims.id eq claimKey }
    }

    suspend fun upsert(claim: VCClaim): VCClaim? = newSuspendedTransaction(Dispatchers.IO) {
        if(claim.key == -1) {
            // new claim
            val insertedId = VCClaims.insertIgnoreAndGetId {
                it[playerKey] = claim.playerKey
                it[displayName] = claim.displayName
                it[lastModified] = claim.lastModified
            }
            log("Inserted new claim for playerKey ${claim.playerKey}, assigned key: ${insertedId?.value}")

            return@newSuspendedTransaction VCClaims.selectAll()
                .where { VCClaims.id eq insertedId?.value }
                .limit(1)
                .firstOrNull()
                ?.let(::rowToVCClaim)

        }else{
            //existing claim
            VCClaims.update({ VCClaims.id eq claim.key }) { st ->
                st[playerKey] = claim.playerKey
                st[displayName] = claim.displayName
                st[lastModified] = claim.lastModified
            }
            log("Updated claim with key ${claim.key} for playerKey ${claim.playerKey}")
            return@newSuspendedTransaction claim //assume the claim is valid
        }
    }

    suspend fun all(): List<VCClaim> = newSuspendedTransaction {
        VCClaims.selectAll().let{ results ->
            results.map(::rowToVCClaim)
        }
    }
}