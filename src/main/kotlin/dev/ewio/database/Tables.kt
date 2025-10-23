package dev.ewio.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object VCPlayers : IntIdTable("vc_player") {
    val mcUuid = varchar("mc_uuid", 36)
    val name = varchar("name", 64)
    val resolvedNameAt = long("resolved_name_at")
    val autoClaim = bool("auto_claim").default(false)

}


object VCClaims : IntIdTable("vc_claim") {
    val playerKey = reference("player_key", VCPlayers, onDelete = ReferenceOption.CASCADE)
    val displayName = varchar("display_name", 128)
    val lastModified = long("last_modified")
}


object VCChunks : IntIdTable("vc_chunk") {
    val claimKey = reference("claim_key", VCClaims, onDelete = ReferenceOption.CASCADE)
    val world = varchar("world", 128)
    val x = integer("x")
    val z = integer("z")

    init { uniqueIndex("ux_chunk_world_x_z", world, x, z) }
}