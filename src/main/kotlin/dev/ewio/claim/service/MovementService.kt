package dev.ewio.claim.service

import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCMovementContext
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.claim.definitions.VCResult
import dev.ewio.listener.MoveListener
import dev.ewio.util.VCCache
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID
import kotlin.to

/**
 * Service for handling player movement related to claims.
 *
 */

class MovementService(
    val registerListener: (listener: MoveListener) -> Unit,
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val cc: CentralCache,
    private val getStringFromConfig: (key: String) -> String,
    private val ms: MessageService
) {
    var moveListener: MoveListener

    init {
        moveListener = MoveListener(
            onMoveChunk = { event ->
                onPlayerMoveChunk(event)
            }
        )
        registerListener(moveListener)
        preService.registerMovementService(this)
    }

    suspend fun autoclaimActivated(player:Player, context: VCPlayerContext, chunk: PlainChunk) {
        actOnAutoclaimResult(
            player = player,
            result = preService.handleAutoClaimOnMove(context, chunk),
            context = context
        )
    }

    suspend fun actOnAutoclaimResult(player: Player, result: VCResult, context: VCPlayerContext){
        when(result){
            is VCResult.UnknownFailure -> {
                log("Unknown Failure")
                ms.send(player, "unknown-error")
            }
            is VCResult.MissingPermission -> {
                ms.send(player, "missing-permission")
            }
            is VCResult.AutoClaim.AutoClaimFailedNoTargetClaimSet -> {
                ms.send(player, "autoclaim.no-target-claim-set")
                preService.disableAutoclaim(context)
            }
            is VCResult.AutoClaim.ChunkLimitReached -> {
                ms.send(player, "claim.max-chunks-reached", mapOf("max_chunks" to context.restrictions.maxChunks.toString()))
                preService.disableAutoclaim(context)
            }
            is VCResult.AutoClaim.ChunkCanNotBeClaimed -> {
                ms.send(player, "claim.can-not-be-claimed")
            }
            is VCResult.AutoClaim.ChunkClaimedByOtherPlayer -> {
                ms.send(player, "claim.claimed-by-other")
            }
            is VCResult.AutoClaim.ChunkAlreadyClaimed -> {
                ms.send(player, "claim.claimed-already")
            }
            is VCResult.AutoClaim.ChunkBelongsToDifferentClaim -> {
                ms.send(player, "autoclaim.chunk-belongs-to-different-claim", mapOf(
                    "chunk_x" to result.chunk.x.toString(),
                    "chunk_z" to result.chunk.z.toString(),
                    "claim_name" to result.otherClaimName
                ))
            }
            is VCResult.AutoClaim.ChunkClaimed -> {
                ms.send(player, "claim.success", mapOf(
                    "chunk_x" to result.chunk.plainChunk.x.toString(),
                    "chunk_z" to result.chunk.plainChunk.z.toString(),
                    "claim_name" to result.claim.displayName,
                    "player" to player.name
                ))
            }
            else -> {
                //if this is reached I have forgotten to handle a case
                log("Unhandled result in autoclaim on move for player ${player.name}. Result: ${result::class.simpleName}")
            }
        }
    }

    private fun onPlayerMoveChunk(event: PlayerMoveEvent) {
        // Handle player moving between chunks
        log("Player ${event.player.name} moved from ${event.from.chunk.x},${event.from.chunk.z} to chunk: ${event.to.chunk.x},${event.to.chunk.z}")

        coroutineScope.launch {
            val context = cc.getPlayerContext(event.player)

            //log("Fetched context for player ${event.player.name}: $context")

            if(context != null) {
                if(context.player.autoClaim){
                    val result = preService.handleAutoClaimOnMove(context, PlainChunk.fromBukkitChunk(event.to.chunk))
                    actOnAutoclaimResult(event.player, result, context)
                }
                if(context.player.bossbar){
                    //TODO: handle bossbar
                }
            }
        }
    }



}