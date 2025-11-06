package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCMovementContext
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.listener.MoveListener
import dev.ewio.util.VCCache
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID

/**
 * Service for handling player movement related to claims.
 *
 */

class MovementService(
    val registerListener: (listener: MoveListener) -> Unit,
    val preService: PrerequisiteService,
    val coroutineScope: CoroutineScope,
    val cc: CentralCache
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

    private fun onPlayerMoveChunk(event: PlayerMoveEvent) {
        // Handle player moving between chunks
        log("Player ${event.player.name} moved from ${event.from.chunk.x},${event.from.chunk.z} to chunk: ${event.to.chunk.x},${event.to.chunk.z}")

        coroutineScope.launch {
            val context = cc.getPlayerContext(event.player)

            if(context != null) {
                if(context.player.autoClaim){
                    preService.handleAutoClaimOnMove(context, event)
                }
                if(context.player.bossbar){
                    //TODO: handle bossbar
                }
            }
        }
    }



}