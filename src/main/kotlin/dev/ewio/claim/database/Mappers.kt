package dev.ewio.claim.database

import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCColor
import dev.ewio.claim.definitions.VCColorLookup
import dev.ewio.claim.definitions.VCColorType
import dev.ewio.claim.definitions.VCLoadedChunk
import dev.ewio.claim.definitions.VCPlayer
import dev.ewio.claim.definitions.VCPoint
import org.jetbrains.exposed.sql.ResultRow
import java.util.UUID

fun rowToVCPlayer(row: ResultRow) = VCPlayer(
    key = row[VCPlayers.id].value,
    mcUUID = UUID.fromString(row[VCPlayers.mcUUID]),
    name = row[VCPlayers.name],
    resolvedNameAt = row[VCPlayers.resolvedNameAt],
    autoClaim = row[VCPlayers.autoClaim],
    autoClaimTargetClaimKey = row[VCPlayers.autoClaimTargetClaimKey],
    bossbar = row[VCPlayers.bossbar]
)

fun rowToVCClaim(row: ResultRow) = VCClaim(
    key = row[VCClaims.id].value,
    playerKey = row[VCClaims.playerKey],
    displayName = row[VCClaims.displayName],
    color = VCColorLookup.colorFromIdentifier(row[VCClaims.colorType]),
    description = row[VCClaims.description],
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

fun rowToVCLoadedChunk(row: ResultRow) = VCLoadedChunk(
    key = row[VCLoadedChunks.id].value,
    playerKey = row[VCLoadedChunks.playerKey],
    playerLocation = VCPoint.fromKey(row[VCLoadedChunks.playerLocation])!!,
    chunk = PlainChunk.fromKey(row[VCLoadedChunks.chunk])!!,
    firstLoaded = row[VCLoadedChunks.firstLoaded],
    name = row[VCLoadedChunks.name]
)