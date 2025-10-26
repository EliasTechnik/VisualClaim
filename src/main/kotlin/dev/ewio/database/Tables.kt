package dev.ewio.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object VCPlayers : IntIdTable("vc_player") {
    //val key = integer("key").uniqueIndex()
    val mcUUID = varchar("mc_uuid", 36)
    val name = varchar("name", 16)
    val resolvedNameAt = long("resolved_name_at")
    val autoClaim = bool("auto_claim").default(false)

    //override val primaryKey = PrimaryKey(key)
}


object VCClaims : IntIdTable("vc_claim") {
    //val key = integer("key").uniqueIndex()
    val playerKey = integer("player_key").references(
        VCPlayers.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    )
    val displayName = varchar("display_name", 250)
    val lastModified = long("last_modified")

    init {
        // z.B. pro Player ein Name nur einmal (optional):
        index(isUnique = true, columns = arrayOf(playerKey, displayName))
    }

    //override val primaryKey = PrimaryKey(key)
}


object VCChunks : IntIdTable("vc_chunk") {
    //val key = integer("key").uniqueIndex()
    val claimKey = integer("claim_key").references(
        VCClaims.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    )
    // PlainChunk
    val world = varchar("world", 64)
    val x = integer("x")
    val z = integer("z")

    init {
        // derselbe Chunk darf nur einmal existieren
        index(isUnique = true, columns = arrayOf(claimKey, world, x, z))
    }

    //override val primaryKey = PrimaryKey(key)
}