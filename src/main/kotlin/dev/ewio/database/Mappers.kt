package dev.ewio.database

import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCPlayer
import org.jetbrains.exposed.sql.ResultRow
import java.util.UUID

fun rowToVCPlayer(row: ResultRow) = VCPlayer(
    key = row[VCPlayers.id].value,
    mcUUID = UUID.fromString(row[VCPlayers.mcUUID]),
    name = row[VCPlayers.name],
    resolvedNameAt = row[VCPlayers.resolvedNameAt],
    autoClaim = row[VCPlayers.autoClaim]
)

fun rowToVCClaim(row: ResultRow) = VCClaim(
    key = row[VCClaims.id].value,
    playerKey = row[VCClaims.playerKey],
    displayName = row[VCClaims.displayName],
    lastModified = row[VCClaims.lastModified]
)

fun rowToVCChunk(row: ResultRow) = VCChunk(
    key = row[VCChunks.id].value,
    claimKey = row[VCChunks.claimKey],
    plainChunk = PlainChunk(
        world = row[VCChunks.world],
        x = row[VCChunks.x],
        z = row[VCChunks.z]
    )
)