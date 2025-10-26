package dev.ewio.claim.repository

import dev.ewio.claim.definitions.*
import dev.ewio.database.VCPlayers
import dev.ewio.database.rowToVCPlayer
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class PlayerRepository {

    suspend fun upsert(player: VCPlayer): VCPlayer? = newSuspendedTransaction(Dispatchers.IO) {
        if(player.key == -1) {
            // new player
            val insertedId = VCPlayers.insertIgnore {
                it[mcUUID] = player.mcUUID.toString()
                it[name] = player.name
                it[resolvedNameAt] = player.resolvedNameAt
                it[autoClaim] = player.autoClaim
            } get VCPlayers.key

            if (insertedId != null) {
                return@newSuspendedTransaction findByKey(insertedId)
            }

            // Fallback: Insert wurde ignoriert oder DB hat keinen Key zurückgegeben -> nach UUID suchen
            return@newSuspendedTransaction findByUUID(player.mcUUID)
        }else{
            //existing player
            VCPlayers.update({ VCPlayers.key eq player.key }) { st ->
                st[name] = player.name
                st[resolvedNameAt] = player.resolvedNameAt
                st[autoClaim] = player.autoClaim
            }
            return@newSuspendedTransaction findByKey(player.key)
        }
    }

    suspend fun findByKey(key: Int): VCPlayer? = newSuspendedTransaction(Dispatchers.IO) {
        VCPlayers.selectAll().where { VCPlayers.key eq key }.limit(1).firstOrNull()?.let(::rowToVCPlayer)
    }

    suspend fun findByUUID(uuid: UUID): VCPlayer? = newSuspendedTransaction(Dispatchers.IO) {
        VCPlayers.selectAll().where { VCPlayers.mcUUID eq uuid.toString() }.limit(1).firstOrNull()?.let(::rowToVCPlayer)
    }
}