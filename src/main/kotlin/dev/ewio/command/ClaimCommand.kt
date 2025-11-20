package dev.ewio.command

import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.service.MessageService
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.GL
import dev.ewio.util.getCorrectlySplitArgs
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class ClaimCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val getStringFromConfig: (key: String) -> String,
    private val ms: MessageService
): TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        coroutineScope.launch {
            val betterArgs = getCorrectlySplitArgs(args.toList(),0)

            preService.getPlayerContext(sender)?.let{
                var (context, realPlayer) = it
                val chunk = PlainChunk.fromBukkitChunk(realPlayer.location.chunk)

                //log("Player ${context.player.name} (${context.player.mcUUID}) is attempting to claim chunk X:${chunk.x} Z:${chunk.z} in world ${chunk.world} with args: $betterArgs")

                val result = if(betterArgs.isEmpty()) {
                    //no name given, use last claim or show usage
                    preService.createClaim(
                        context = context,
                        chunk = chunk
                    )
                } else{
                    //we have a name
                    preService.createClaim(
                        context = context,
                        chunk = chunk,
                        claimString = betterArgs[0]
                    )
                }

                //result handling here
                when(result) {
                    is VCResult.CreateClaim.ClaimCreatedSuccessfully -> {
                        ms.send(
                            player = realPlayer,
                            key = "claim.success",
                            placeholders = mapOf(
                                "chunk_x" to result.chunk.plainChunk.x.toString(),
                                "chunk_z" to result.chunk.plainChunk.z.toString(),
                                "player" to context.player.name,
                                "claim_name" to result.claim.displayName
                            )
                        )
                        preService.updateBossbarAfterClaim(realPlayer, context, result.claim)
                    }
                    is VCResult.CreateClaim.ChunkTransferredToClaim -> {
                        ms.send(
                            player = realPlayer,
                            key = "claim.addedToClaim",
                            placeholders = mapOf(
                                "chunk_x" to result.chunk.x.toString(),
                                "chunk_z" to result.chunk.z.toString(),
                                "player" to context.player.name,
                                "claim_name" to result.claim.displayName
                            )
                        )
                        preService.updateBossbarAfterClaim(realPlayer, context, result.claim)
                    }
                    is VCResult.CreateClaim.ChunkAlreadyClaimedBySameClaim -> {
                        ms.send(
                            player = realPlayer,
                            key = "claim.claimed-already"
                        )
                    }
                    is VCResult.CreateClaim.ChunkClaimedByOtherPlayer -> {
                        ms.send(realPlayer, "claim.claimed-by-other")
                    }
                    is VCResult.CreateClaim.ChunkLimitReached -> {
                        ms.send(realPlayer, "claim.max-chunks-reached", mapOf("max-chunks" to result.maxChunks.toString()))
                    }
                    is VCResult.CreateClaim.ClaimLimitReached -> {
                        ms.send(realPlayer, "claim.max-claims-reached", mapOf("max-claims" to result.maxClaims.toString()))
                    }
                    is VCResult.CreateClaim.ChunkCanNotBeClaimed -> {
                        ms.send(realPlayer, "claim.can-not-be-claimed")
                    }
                    is VCResult.CreateClaim.NoExistingClaimFound -> {
                        ms.send(realPlayer, "usage.claim")
                    }
                    is VCResult.CreateClaim.ClaimNameTooLong -> {
                        ms.send(realPlayer, "claim.name-too-long", mapOf("max-length" to result.maxLength.toString()))
                    }
                    is VCResult.MissingPermission -> {
                        ms.send(realPlayer, "missing-permission")
                    }
                    is VCResult.CreateClaim.ClaimNameNotAllowed -> {
                        ms.send(realPlayer, "claim.claim-name-not-allowed")
                    }
                    else -> {
                        log("Unknown Failure")
                        ms.send(realPlayer, "unknown-error")
                    }
                }
            }
        }
        return true //we handle everything in the coroutine
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String>?{
        val betterArgs = getCorrectlySplitArgs(args.toList(),0)
        val player = sender as? Player?: return mutableListOf()

        preService.getCachedPlayerContext(player)?.let{ context ->
            //get available claims
            val names = context.claims.map {"\"" + it.displayName +"\"" }

            if (names.isEmpty()) {
                return mutableListOf()
            }

            return when (betterArgs.size) {
                1 -> {
                    names.toMutableList()
                }
                else -> mutableListOf()
            }
        }
        return mutableListOf()
    }
}