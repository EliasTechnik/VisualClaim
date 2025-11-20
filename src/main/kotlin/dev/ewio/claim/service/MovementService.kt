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
    val preService: PrerequisiteService,
    val coroutineScope: CoroutineScope,
    val cc: CentralCache,
    private val getStringFromConfig: (key: String) -> String
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
                player.sendMessage(getStringFromConfig("messages.unknown-error"))
            }
            is VCResult.MissingPermission -> {
                player.sendMessage(getStringFromConfig("messages.missing-permission"))
            }
            is VCResult.AutoClaim.AutoClaimFailedNoTargetClaimSet -> {
                player.sendMessage(getStringFromConfig("messages.autoclaim.no-target-claim-set"))
                preService.disableAutoclaim(context)
            }
            is VCResult.AutoClaim.ChunkLimitReached -> {
                player.sendMessage(
                    getStringFromConfig("messages.claim.max-chunks-reached")
                        .replace("<max-chunks>", context.restrictions.maxChunks.toString())
                )
                preService.disableAutoclaim(context)
            }
            is VCResult.AutoClaim.ChunkCanNotBeClaimed -> {
                player.sendMessage(
                    getStringFromConfig("messages.claim.can-not-be-claimed")
                )
            }
            is VCResult.AutoClaim.ChunkClaimedByOtherPlayer -> {
                player.sendMessage(getStringFromConfig("messages.claim.claimed-by-other"))
            }
            is VCResult.AutoClaim.ChunkAlreadyClaimed -> {
                player.sendMessage(
                    getStringFromConfig("messages.claim.claimed-already")
                )
            }
            is VCResult.AutoClaim.ChunkBelongsToDifferentClaim -> {
                player.sendMessage(getStringFromConfig("messages.autoclaim.chunk-belongs-to-different-claim")
                    .replace("<x>", result.chunk.x.toString())
                    .replace("<z>", result.chunk.z.toString())
                    .replace("<claim-name>", result.otherClaimName))
            }
            is VCResult.AutoClaim.ChunkClaimed -> {
                player.sendMessage(getStringFromConfig("messages.claim.success")
                    .replace("<x>", result.chunk.plainChunk.x.toString())
                    .replace("<z>", result.chunk.plainChunk.z.toString())
                    .replace("<claim-name>", result.claim.displayName)
                    .replace("<player>", player.name)
                )
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