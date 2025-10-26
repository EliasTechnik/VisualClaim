package dev.ewio.command

import dev.ewio.VisualClaim
import dev.ewio.claim.service.ClaimService
import dev.ewio.claim.service.PrerequisiteService
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
    private val getStringFromConfig: (key: String) -> String
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
                        //TODO: List other player's claims

                        val otherPlayer = getPlayer(betterArgs[0])

                        otherPlayer?.let {
                            preService.getCachedPlayerContext(it)?.let { targetContext ->
                                if (targetContext.claims.isEmpty()) {
                                    realPlayer.sendMessage(
                                        getStringFromConfig("messages.list-claims.no-claims-other")
                                            .replace("<player>", betterArgs[0])
                                    )
                                } else {
                                    realPlayer.sendMessage(
                                        getStringFromConfig("messages.list-claims.header-other")
                                            .replace("<player>", betterArgs[0])
                                    )
                                    for (claim in targetContext.claims) {
                                        if (getStringFromConfig("plugin-insights.enabled").toBoolean()) {
                                            realPlayer.sendMessage("§6- ${claim.displayName} (ID: ${claim.key})")
                                        } else {
                                            realPlayer.sendMessage("§6- ${claim.displayName}")
                                        }
                                    }
                                    realPlayer.sendMessage(
                                        getStringFromConfig("messages.list-claims.summary-other")
                                            .replace(
                                                "<player>",
                                                betterArgs[0]
                                            )
                                            .replace(
                                                "<chunk-count>",
                                                targetContext.chunks.size.toString()
                                            )
                                            .replace(
                                                "<maxchunks>",
                                                targetContext.restrictions.maxChunks.toString()
                                            )
                                            .replace(
                                                "<claim-count>",
                                                targetContext.claims.size.toString()
                                            )
                                            .replace(
                                                "<maxclaims>",
                                                targetContext.restrictions.maxClaims.toString()
                                            )
                                    )
                                }
                            } ?: run {
                                realPlayer.sendMessage(
                                    getStringFromConfig("messages.player-not-found")
                                        .toString()
                                        .replace("<player>", betterArgs[0])
                                )
                            }
                        }?: run {
                            realPlayer.sendMessage(
                                getStringFromConfig("messages.player-not-found")
                                    .toString()
                                    .replace("<player>", betterArgs[0])
                            )
                        }
                    }
                }else{
                    //list own claims
                    if(context.claims.isEmpty()){
                        realPlayer.sendMessage(getStringFromConfig("messages.no-claims").toString())
                    } else {
                        realPlayer.sendMessage(getStringFromConfig("messages.list-claims.header").toString())
                        for(claim in context.claims){
                            if(getStringFromConfig("plugin-insights.enabled").toBoolean()){
                                realPlayer.sendMessage("§6- ${claim.displayName} (ID: ${claim.key})")
                            }else{
                                realPlayer.sendMessage("§6- ${claim.displayName}")
                            }
                        }
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.list-claims.summary")
                                .toString()
                                .replace(
                                    "<chunk-count>",
                                    context.chunks.size.toString())
                                .replace(
                                    "<maxchunks>",
                                    context.restrictions.maxChunks .toString())
                                .replace(
                                    "<claim-count>",
                                    context.claims.size.toString())
                                .replace(
                                    "<maxclaims>",
                                    context.restrictions.maxClaims .toString())
                        )
                    }
                }
            }
        }
        return true
    }
}

