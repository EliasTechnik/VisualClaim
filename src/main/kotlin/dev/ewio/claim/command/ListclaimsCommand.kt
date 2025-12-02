package dev.ewio.claim.command

import dev.ewio.claim.service.MessageService
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.countChunksInClaim
import dev.ewio.util.getCorrectlySplitArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit.getPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class ListclaimsCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val getStringFromConfig: (key: String) -> String,
    private val ms: MessageService
): TabExecutor {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {

        val betterArgs = getCorrectlySplitArgs(args.toList(),0)
        val player = sender as? Player?: return mutableListOf()

        preService.getCachedPlayerContext(player)?.let { context ->
            if(context.restrictions.listOtherPlayerClaims){
                if(betterArgs.size == 1){
                    val partialName = betterArgs[0].lowercase()
                    val matchingPlayerNames = preService.getCachedPlayerNames().filter {
                        it.lowercase().startsWith(partialName)
                    }
                    return matchingPlayerNames.toMutableList()
                }
            }
        }
        return mutableListOf()
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        coroutineScope.launch {
            val betterArgs = getCorrectlySplitArgs(args.toList(),0)

            preService.getPlayerContext(sender)?.let {
                var (context, realPlayer) = it

                //check if listing other players claims
                if(betterArgs.isNotEmpty()){
                    if(context.restrictions.listOtherPlayerClaims){

                        val otherPlayer = getPlayer(betterArgs[0])

                        otherPlayer?.let {
                            preService.getCachedPlayerContext(it)?.let { targetContext ->
                                if (targetContext.claims.isEmpty()) {
                                    ms.send(realPlayer, "list-claims.no-claims-other", mapOf("player" to betterArgs[0]))
                                } else {
                                    realPlayer.sendMessage(
                                        getStringFromConfig("messages.list-claims.header-other")
                                            .replace("<player>", betterArgs[0])
                                    )
                                    ms.send(realPlayer, "list-claims.header-other", mapOf("player" to betterArgs[0]))
                                    for (claim in targetContext.claims) {
                                        ms.send(realPlayer, "list-claims.entry", mapOf(
                                            "claim_name" to claim.displayName,
                                            "chunk_count" to countChunksInClaim(targetContext, claim).toString()
                                            )
                                        )
                                    }
                                    ms.send(realPlayer, "list-claims.summary-other", mapOf(
                                        "player" to betterArgs[0],
                                        "chunk_count" to targetContext.chunks.size.toString(),
                                        "max_chunks" to targetContext.restrictions.maxChunks.toString(),
                                        "claim_count" to targetContext.claims.size.toString(),
                                        "max_claims" to targetContext.restrictions.maxClaims.toString()
                                        )
                                    )
                                }
                            } ?: run {
                                ms.send(realPlayer, "player-not-found", mapOf("player" to betterArgs[0]))
                            }
                        }?: run {
                            ms.send(realPlayer, "player-not-found", mapOf("player" to betterArgs[0]))
                        }
                    }
                }else{
                    //list own claims
                    if(context.claims.isEmpty()){
                        ms.send(realPlayer, "no-claims")
                    } else {
                        realPlayer.sendMessage(getStringFromConfig("messages.list-claims.header").toString())
                        for(claim in context.claims){
                            ms.send(realPlayer, "list-claims.entry", mapOf(
                                "claim_name" to claim.displayName,
                                "chunk_count" to countChunksInClaim(context, claim).toString()
                                )
                            )
                        }
                        ms.send(realPlayer, "list-claims.summary", mapOf(
                            "chunk_count" to context.chunks.size.toString(),
                            "max_chunks" to context.restrictions.maxChunks.toString(),
                            "claim_count" to context.claims.size.toString(),
                            "max_claims" to context.restrictions.maxClaims.toString()
                            )
                        )
                    }
                }
            }
        }
        return true
    }
}

