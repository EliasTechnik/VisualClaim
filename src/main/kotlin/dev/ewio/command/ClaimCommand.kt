package dev.ewio.command

import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCResult
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

                log("Player ${context.player.name} (${context.player.mcUUID}) is attempting to claim chunk X:${chunk.x} Z:${chunk.z} in world ${chunk.world} with args: $betterArgs")

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
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim.success")
                                .replace("<x>", result.chunk.plainChunk.x.toString())
                                .replace("<z>", result.chunk.plainChunk.z.toString())
                                .replace("<player>",context.player.name)
                                .replace("<claim-name>",result.claim.displayName)
                        )
                    }
                    is VCResult.CreateClaim.ChunkTransferredToClaim -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim.addedToClaim")
                                .replace("<x>", result.chunk.x.toString())
                                .replace("<z>", result.chunk.z.toString())
                                .replace("<player>",context.player.name)
                                .replace("<claim-name>",result.claim.displayName)
                        )
                    }
                    is VCResult.CreateClaim.ChunkAlreadyClaimedBySameClaim -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim.claimed-already")
                        )
                    }
                    is VCResult.CreateClaim.ChunkClaimedByOtherPlayer -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim.claimed-by-other")
                                .replace("<other-player>", result.otherPlayer)
                        )
                    }
                    is VCResult.CreateClaim.ChunkLimitReached -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim.max-chunks-reached")
                                .replace("<max-chunks>", result.maxChunks.toString())
                        )
                    }
                    is VCResult.CreateClaim.ClaimLimitReached -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim.max-claims-reached")
                                .replace("<max-claims>", result.maxClaims.toString())
                        )
                    }
                    is VCResult.CreateClaim.ChunkCanNotBeClaimed -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim.can-not-be-claimed")
                        )
                    }
                    is VCResult.CreateClaim.NoExistingClaimFound -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("usage.claim")
                        )
                    }
                    is VCResult.CreateClaim.ClaimNameTooLong -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim.name-too-long")
                                .replace("<max-length>", result.maxLength.toString())
                        )
                    }
                    is VCResult.MissingPermission -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.missing-permission")
                        )
                    }
                    else -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.unknown-error")
                        )
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