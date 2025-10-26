package dev.ewio.command

import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.getCorrectlySplitArgs
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


                var result: VCResult
                if(betterArgs.isEmpty()) {
                    //no name given, use last claim or show usage
                    result = preService.createClaim(
                        context = context,
                        chunk = chunk
                    )
                }
                else{
                    result = preService.createClaim(
                        context = context,
                        chunk = chunk,
                        claimName = betterArgs[0]
                    )
                }

                //update context after operation
                val newContext = preService.updatePlayerContext(context)

                if(newContext != null){
                    context = newContext
                }else{
                    //could not update context
                    realPlayer.sendMessage(
                        getStringFromConfig("messages.error.unknown-error")
                    )
                    return@launch
                }

                //result handling here
                when(result) {
                    is VCResult.CreateClaim.ChunkClaimedSucessfully -> {
                        val claim = context.claims.maxByOrNull { it.lastModified }
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim-success")
                                .replace("<x>", chunk.x.toString())
                                .replace("<z>", chunk.z.toString())
                                .replace("<player>",context.player.name)
                                .replace("<claim-name>",claim?.displayName ?: "Unnamed Claim")
                        )
                    }
                    is VCResult.CreateClaim.NoExistingClaimFound -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("usage.claim")
                        )
                    }
                    is VCResult.CreateClaim.ChunkLimitReached -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.error.chunk-limit-reached")
                                .replace("<max-chunks>", result.maxChunks.toString())
                        )
                    }
                    is VCResult.CreateClaim.ChunkCouldNotBeClaimed -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.error.chunk-could-not-be-claimed")
                        )
                    }
                    is VCResult.CreateClaim.ClaimCouldNotBeCreated -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.error.claim-could-not-be-created")
                        )
                    }
                    is VCResult.CreateClaim.ClaimLimitReached -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.error.claim-limit-reached")
                                .replace("<max-claims>", result.maxClaims.toString())
                        )
                    }
                    is VCResult.CreateClaim.ClaimNameTooLong -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.error.claim-name-too-long")
                                .replace("<max-length>", result.maxLength.toString())
                        )
                    }
                    is VCResult.CreateClaim.ChunkAlreadyClaimedBySameClaim -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.error.chunk-already-claimed-by-same-claim")
                        )
                    }
                    is VCResult.CreateClaim.ChunkClaimedByOtherPlayer -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.error.chunk-claimed-by-other-player")
                                .replace("<other-player>", result.otherPlayer)
                        )
                    }
                    else -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.error.unknown-error")
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
    ): MutableList<String>? {
        // Recommendations für <arg>

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