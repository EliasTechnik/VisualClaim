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
 *  /claimlore get <claimname> - Retuns the lore of the specified claim as a cliackable text which copys it to the clipboard
 *  /claimlore set <claimname> <lore...> - Sets the lore of the specified claim
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

                val result = when(betterArgs.size){
                    //get
                    2 -> {
                        if(betterArgs[0].equals("get", ignoreCase = true)){

                            val claim = context.claims.find{ it.displayName.equals(betterArgs[1], ignoreCase = true) }

                            if(claim != null){
                                VCResult.ClaimLore.LoreGet(claim)
                            }else{
                                VCResult.ClaimLore.ClaimNotFound
                            }
                        }
                        else{
                            VCResult.MalformedCommand
                        }
                    }
                    //set
                    3 -> {
                        if(betterArgs[0].equals("set", ignoreCase = true)){
                            val claim = context.claims.find{ it.displayName.equals(betterArgs[1], ignoreCase = true) }

                            if(claim != null){
                                preService.updateClaimDescription(
                                    context = context,
                                    claim = claim,
                                    newDescription = betterArgs[2]
                                )
                            }else{
                                ms.send(realPlayer, "claim-not-found")
                            }
                        }
                        else{
                            VCResult.MalformedCommand
                        }
                    }
                    else -> {
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
                            "claim_lore" to result.claim.description
                        ))
                    }
                    is VCResult.ClaimLore.LoreGet -> {
                        ms.send(realPlayer, "claimlore.get", mapOf(
                            "claim_name" to result.claim.displayName,
                            "claim_lore" to result.claim.description
                        ))
                    }
                    is VCResult.ClaimLore.ClaimNotFound -> {
                        ms.send(realPlayer, "claim-not-found")
                    }
                    is VCResult.ClaimLore.ContainsInvalidCharacters -> {
                        ms.send(realPlayer, "claimlore.invalid-characters")
                    }
                    is VCResult.ClaimLore.LoreTooLong -> {
                        ms.send(realPlayer, "claimlore.lore-too-long", mapOf(
                            "max_length" to result.maxLength.toString()
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