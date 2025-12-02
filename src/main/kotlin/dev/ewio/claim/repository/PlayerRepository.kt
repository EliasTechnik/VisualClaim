package dev.ewio.claim.repository

import dev.ewio.claim.definitions.*
import dev.ewio.claim.database.VCPlayers
import dev.ewio.claim.database.rowToVCPlayer
import dev.ewio.util.log
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class PlayerRepository {

    suspend fun upsert(player: VCPlayer): VCPlayer? = newSuspendedTransaction(Dispatchers.IO) {
        if(player.key == -1) {
            // new player
            val insertedId = VCPlayers.insertIgnoreAndGetId {
                it[mcUUID] = player.mcUUID.toString()
                it[name] = player.name
                it[resolvedNameAt] = player.resolvedNameAt
                it[autoClaim] = player.autoClaim
                it[bossbar] = player.bossbar
                it[autoClaimTargetClaimKey] = player.autoClaimTargetClaimKey
            }

            log("Inserted new player with UUID ${player.mcUUID}, assigned key: ${insertedId?.value}")

            return@newSuspendedTransaction VCPlayers
                .selectAll()
                .where { VCPlayers.id eq insertedId?.value }
                .limit(1)
                .firstOrNull()?.let(::rowToVCPlayer)
        }else{
            //existing player
            VCPlayers.update({ VCPlayers.id eq player.key }) { st ->
                st[name] = player.name
                st[resolvedNameAt] = player.resolvedNameAt
                st[autoClaim] = player.autoClaim
                st[bossbar] = player.bossbar
                st[autoClaim] = player.autoClaim
                st[bossbar] = player.bossbar
                st[autoClaimTargetClaimKey] = player.autoClaimTargetClaimKey
            }
            return@newSuspendedTransaction player
        }
    }

    suspend fun findByKey(key: Int): VCPlayer? = newSuspendedTransaction(Dispatchers.IO) {
        VCPlayers.selectAll().where { VCPlayers.id eq key }.limit(1).firstOrNull()?.let(::rowToVCPlayer)
    }

    suspend fun findByUUID(uuid: UUID): VCPlayer? = newSuspendedTransaction(Dispatchers.IO) {
        VCPlayers.selectAll().where { VCPlayers.mcUUID eq uuid.toString() }.limit(1).firstOrNull()?.let(::rowToVCPlayer)
    }

    suspend fun all(): List<VCPlayer> = newSuspendedTransaction {
        VCPlayers.selectAll().let{ results ->
            results.map(::rowToVCPlayer)
        }
    }

    suspend fun findByName(playerName: String): List<VCPlayer> = newSuspendedTransaction {
        VCPlayers.selectAll().where { VCPlayers.name eq playerName }.map(::rowToVCPlayer)
    }
}