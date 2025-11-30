package dev.ewio.command

import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.service.MessageService
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.getCorrectlySplitArgs
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import kotlin.collections.mutableListOf

/**
 * Deletes a claim owned by the player.
 *
 * Usage:
 * /deleteclaim <claim-name> <confirmation>
 * /deleteclaim -p <player> <claim-name> <confirmation> [if player has permission VisualClaim.deleteOther]
 *
 *
 * If no arguments are provided, the command shows usage information.
 * The player must confirm the deletion by providing the correct confirmation word.
 */

class DeleteclaimCommand(
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
            val betterArgs = getCorrectlySplitArgs(args.toList(), 0)

            preService.getPlayerContext(sender)?.let {
                val (context, realPlayer) = it

                val result = when (betterArgs.size){
                    0 -> {
                        VCResult.MalformedCommand
                    }
                    1 -> {
                        //claim name given
                        //check if claim exists
                        if(betterArgs[0].startsWith("-o")){
                            //missing playername
                            VCResult.MalformedCommand
                        }else {
                            //check if claim exists
                            if (context.claims.firstOrNull { it.displayName == betterArgs[0] } == null) {
                                VCResult.DeleteClaim.VCClaimNotFound(betterArgs[0])
                            } else {
                                VCResult.DeleteClaim.ConfirmationRequired(betterArgs[0])
                            }
                        }
                    }
                    2 -> {
                        //claim name + confirmation or "-o" + playername

                        if(betterArgs[0].startsWith("-o")){
                            //check if playername exists
                            VCResult.MalformedCommand //This is not very user friendly, but ok for now
                            //TODO: improve this so that admins get proper messages
                        }else{
                            //check if claim exists
                            if (context.claims.firstOrNull { it.displayName == betterArgs[0] } == null) {
                                VCResult.DeleteClaim.VCClaimNotFound(betterArgs[0])
                            } else {
                                if(betterArgs[1].equals(getStringFromConfig("trigger-words.deleteclaim-confirm"), ignoreCase = true)) {
                                    //proceed with deletion
                                    preService.deleteClaim(
                                        context = context,
                                        claimName = betterArgs[0],
                                    )
                                } else {
                                    VCResult.MalformedCommand
                                }
                            }
                        }
                    }
                    3 -> {
                        //"-p" + playername + claimname
                        if(betterArgs[0].startsWith("-p")){
                            //check if playername exists
                            if(preService.getCachedPlayerNames().firstOrNull { it == betterArgs[1] } != null) {
                                //check if claim exists
                                preService.deleteClaim(
                                    context = context,
                                    claimName = betterArgs[2],
                                    playerName = betterArgs[1],
                                    pretestAdmin = true,
                                    adminMode = true
                                )
                            }else{
                                VCResult.MalformedCommand
                            }
                        }else{
                            VCResult.MalformedCommand
                        }
                    }
                    4 -> {
                        //"-o" + playername + claimname + confirmation
                        if(betterArgs[0].startsWith("-p")){
                            //check if playername exists
                            if(preService.getCachedPlayerNames().firstOrNull { it == betterArgs[1] } != null) {
                                //check if claim exists
                                val pretest = preService.deleteClaim(
                                    context = context,
                                    claimName = betterArgs[2],
                                    playerName = betterArgs[1],
                                    pretestAdmin = true,
                                    adminMode = true
                                )

                                if(pretest is VCResult.DeleteClaim.ConfirmOtherPlayerClaimRequired){
                                    if(betterArgs[3].equals(getStringFromConfig("trigger-words.deleteclaim-confirm"), ignoreCase = true)) {
                                        //proceed with deletion
                                        preService.deleteClaim(
                                            context = context,
                                            claimName = betterArgs[2],
                                            playerName = betterArgs[1],
                                            adminMode = true
                                        )
                                    } else {
                                        VCResult.DeleteClaim.ConfirmOtherPlayerClaimRequired(betterArgs[2])
                                    }
                                } else {
                                    pretest
                                }
                            }else{
                                VCResult.MalformedCommand
                            }
                        }else{
                            VCResult.MalformedCommand
                        }
                    }
                    else -> {
                        VCResult.MalformedCommand
                    }
                }


                when(result){
                    is VCResult.DeleteClaim.RemovedSuccessful ->  {
                        ms.send(realPlayer, "deleteclaim.success", mapOf("claim_name" to result.claimName))
                        //preService.updateBossbarAfterClaim(realPlayer, context, null) //TODO: remove if notify callchain works
                    }
                    is VCResult.DeleteClaim.VCClaimNotFound -> {
                        ms.send(realPlayer, "claim-not-found", mapOf("claim_name" to result.claimName))
                    }
                    is VCResult.DeleteClaim.ConfirmationRequired -> {
                        ms.send(
                            player = realPlayer,
                            key = "deleteclaim.confirm",
                            placeholders = mapOf(
                                "claim_name" to result.claimName,
                                "deleteclaim_confirm" to getStringFromConfig("trigger-words.deleteclaim-confirm")
                            )
                        )
                    }
                    is VCResult.DeleteClaim.ConfirmOtherPlayerClaimRequired -> {
                        ms.send(
                            player = realPlayer,
                            key = "deleteclaim.confirm-other",
                            placeholders = mapOf(
                                "claim_name" to result.claimName,
                                "deleteclaim_confirm" to getStringFromConfig("trigger-words.deleteclaim-confirm")
                            )
                        )
                    }
                    is VCResult.DeleteClaim.NotOwnerOfClaim -> {
                        ms.send(
                            player = realPlayer,
                            key = "deleteclaim.not-owner",
                            placeholders = mapOf(
                                "claim_name" to result.claimName,
                            )
                        )
                    }
                    is VCResult.MalformedCommand -> {
                        ms.send(realPlayer, "usage.deleteclaim")
                    }
                    else -> {
                        log("Unknown Failure")
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
        // Recommendations für <arg>
        val betterArgs = getCorrectlySplitArgs(args.toList(), 0)
        val player = sender as? Player ?: return mutableListOf()

        preService.getCachedPlayerContext(player)?.let { context ->
            when (betterArgs.size) {
                1 -> {
                    //claim names
                    val names = context.claims.map { "\"" + it.displayName + "\"" }.toMutableList()
                    if(context.restrictions.deleteclaimOther){
                        names.add("-p")
                    }
                    return names
                }

                2 -> {
                    if (betterArgs[0] == "-p" && context.restrictions.deleteclaimOther) {
                        //player names
                        val playerNames = preService.getCachedPlayerNames().map { it }
                        return playerNames.toMutableList()
                    } else {
                        return mutableListOf()
                    }
                }

                3 -> {
                    if (betterArgs[0] == "-p" && context.restrictions.deleteclaimOther) {
                        //TODO: claim names of the other player
                        //this is a bit complicated because some db access is needed and this is costly to do on tab complete
                        //the best way would be to cache claims per player name in the preService but that is not implemented yet
                        //for now, return empty list
                        return mutableListOf()
                    } else {
                        return mutableListOf()
                    }
                }
                else -> return mutableListOf()
            }
        }
        return mutableListOf()
    }
}