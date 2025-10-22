package dev.ewio.database

import dev.ewio.claim.repository.definitions.PlainChunk
import dev.ewio.claim.repository.definitions.VCChunk
import dev.ewio.claim.repository.definitions.VCClaim
import dev.ewio.claim.repository.definitions.VCPlayer
import dev.ewio.database.VCClaims.playerKey
import dev.ewio.util.chunkKey
import dev.ewio.util.claimKey

import dev.ewio.util.playerKey
import org.jetbrains.exposed.sql.ResultRow
import java.util.UUID

fun ResultRow.toVCPlayer(): VCPlayer = VCPlayer(
    key = playerKey(this[VCPlayers.id].value),
    mcUUID = UUID.fromString(this[VCPlayers.mcUuid]),
    name = this[VCPlayers.name],
    resolvedNameAt = this[VCPlayers.resolvedNameAt],
    autoClaim = this[VCPlayers.autoClaim]
)


fun ResultRow.toVCClaim(): VCClaim = VCClaim(
    key = claimKey(this[VCClaims.id].value),
    playerKey = playerKey(this[playerKey].value),
    displayName = this[VCClaims.displayName],
    lastModified = this[VCClaims.lastModified]
)


fun ResultRow.toVCChunk(): VCChunk = VCChunk(
    key = chunkKey(this[VCChunks.id].value),
    claimKey = claimKey(this[VCChunks.claimKey].value),
    plainChunk = PlainChunk(
        world = this[VCChunks.world],
        x = this[VCChunks.x],
        z = this[VCChunks.z]
    )
)