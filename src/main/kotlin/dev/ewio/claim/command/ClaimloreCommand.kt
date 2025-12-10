package dev.ewio.claim.command

import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.service.MessageService
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.getCorrectlySplitArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

/**
 *  /claimlore get [-p] [>other_player>] <claimname> - Retuns the lore of the specified claim as a cliackable text which copys it to the clipboard
 *  /claimlore set [-p] [<other_player>] <claimname> - Sets the lore of the specified claim (gives the player a link to click to set it)
 *
 */
class ClaimloreCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val ms: MessageService
): TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        coroutineScope.launch {
            val betterArgs = getCorrectlySplitArgs(args.toList(), 0)
            preService.getPlayerContext(sender)?.let {
                val (context, realPlayer) = it

                val result = if(betterArgs.size == 2){
                    //caller is target

                    if(betterArgs[0] == "get"){
                        preService.getClaimLore(
                            context = context,
                            claimName = betterArgs[1]
                        )
                    }else if(betterArgs[0] == "set") {
                        preService.setClaimLore(
                            context = context,
                            claimName = betterArgs[1]
                        )
                    }else{
                        VCResult.MalformedCommand
                    }

                }else{
                    if(betterArgs.size == 4) {
                        //other player target
                        if(betterArgs[0] == "get" && betterArgs[1].equals("-p", ignoreCase = true)) {
                            preService.getClaimLore(
                                context = context,
                                targetPlayerName = betterArgs[2],
                                claimName = betterArgs[3]
                            )
                        } else if(betterArgs[0] == "set" && betterArgs[1].equals("-p", ignoreCase = true)) {
                            preService.setClaimLore(
                                context = context,
                                targetPlayerName = betterArgs[2],
                                claimName = betterArgs[3]
                            )
                        } else{
                            VCResult.MalformedCommand
                        }
                    }
                    else{
                        VCResult.MalformedCommand
                    }
                }

                when(result){
                    is VCResult.MalformedCommand -> {
                        ms.send(
                            player = realPlayer,
                            key = "usage.claimlore"
                        )
                    }
                    is VCResult.ClaimLore.LoreSet -> {
                        ms.send(realPlayer, "claimlore.set", mapOf(
                            "claim_name" to result.claim.displayName,
                            "edit_url" to result.linkToWebeditor,
                            "token_lifetime_minutes" to result.lifetimeMinutes.toString()
                        ))
                    }
                    is VCResult.ClaimLore.LoreSetOther -> {
                        ms.send(realPlayer, "claimlore.set-other", mapOf(
                            "claim_name" to result.claim.displayName,
                            "edit_url" to result.linkToWebeditor,
                            "player" to result.targetPlayerName,
                            "token_lifetime_minutes" to result.lifetimeMinutes.toString()
                        ))
                    }
                    is VCResult.ClaimLore.LoreGet -> {
                        ms.send(realPlayer, "claimlore.get", mapOf(
                            "claim_name" to result.claim.displayName,
                            "claim_lore" to result.claim.description
                        ))
                    }
                    is VCResult.ClaimLore.LoreGetOther -> {
                        ms.send(realPlayer, "claimlore.get-other", mapOf(
                            "claim_name" to result.claim.displayName,
                            "claim_lore" to result.claim.description,
                            "player" to result.targetPlayerName
                        ))
                    }
                    is VCResult.VCClaimNotFound -> {
                        ms.send(realPlayer, "claim-not-found", mapOf(
                            "claim_name" to result.claimName
                        ))
                    }
                    is VCResult.ClaimLore.LoreTooLong -> {
                        ms.send(realPlayer, "claimlore.lore-too-long", mapOf(
                            "max_length" to result.maxLength.toString()
                        ))
                    }
                    is VCResult.VCPlayerNotFound -> {
                        ms.send(realPlayer, "player-not-found", mapOf(
                            "player_name" to result.playerName
                        ))
                    }
                    else -> {
                        ms.send(realPlayer, "unknown-error")
                    }
                }
            }

        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        val betterArgs = getCorrectlySplitArgs(args.toList(),0)
        val player = sender as? Player?: return mutableListOf()

        preService.getCachedPlayerContext(player)?.let { context ->

            when (betterArgs.size) {
                1 -> {
                    val subcommands = listOf("get", "set")
                    return subcommands.filter { it.startsWith(betterArgs[0], ignoreCase = true) }.toMutableList()
                }

                2 -> {
                    val subcommand = betterArgs[0].lowercase()
                    if (subcommand == "get" || subcommand == "set") {
                        // Suggest claim names owned by the player
                        val ownedClaimNames = context.claims.map { it.displayName }
                        return ownedClaimNames.filter { it.startsWith(betterArgs[1], ignoreCase = true) }
                            .map { "\"$it\"" }.toMutableList()
                    }
                }
            }
        }

        return mutableListOf()
    }
}