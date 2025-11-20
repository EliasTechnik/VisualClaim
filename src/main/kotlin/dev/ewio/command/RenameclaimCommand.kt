package dev.ewio.command

import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.getCorrectlySplitArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player


class RenameclaimCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val getStringFromConfig: (key: String) -> String
): TabExecutor {
    /**
     * Renames a claim owned by the player.
     * Usage: /renameclaim <old-claim-name> <new-claim-name>
     * If a claim with the new name already exists, the player will be prompted to confirm a merge.
     * If confirmed, the old-claim will be merged into the new one.
     * Given the right permission: /renameclaim -p <player> <old-claim-name> <new-claim-name>
     * will rename/merge another player's claim.
     */
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        coroutineScope.launch {
            val betterArgs = getCorrectlySplitArgs(args.toList(), 0)

            preService.getPlayerContext(sender)?.let {
                var (context, realPlayer) = it

                val result = when (betterArgs.size) {
                    2 -> {
                        // /renameclaim <old-claim-name> <new-claim-name>
                        // or
                        // /renameclaim -p <player>
                        if (betterArgs[0] == "-p") {
                            //invalid usage
                            realPlayer.sendMessage(
                                getStringFromConfig("usage.renameclaim-other")
                            )
                            return@launch
                        } else {
                            //renaming own claim
                            preService.renameClaim(
                                context = context,
                                oldName = betterArgs[0],
                                newName = betterArgs[1]
                            )
                        }
                    }

                    3 -> {
                        // /renameclaim <old-claim-name> <new-claim-name> confirm
                        // or
                        // /renameclaim -p <player> <old-claim-name>
                        if (betterArgs[0] == "-p") {
                            //invalid usage
                            realPlayer.sendMessage(
                                getStringFromConfig("usage.renameclaim-other")
                            )
                            return@launch
                        } else {
                            //renaming own claim with possible merge confirmation
                            preService.renameClaim(
                                context = context,
                                oldName = betterArgs[0],
                                newName = betterArgs[1],
                                confirmMerge = betterArgs[2].lowercase() == getStringFromConfig("trigger-words.renameclaim-confirm").lowercase()
                            )
                        }
                    }

                    4 -> {
                        // /renameclaim -p <player> <old-claim-name> <new-claim-name>
                        if (betterArgs[0] == "-p") {
                            //renaming other player's claim
                            preService.renameForeignClaim(
                                context = context,
                                targetPlayerName = betterArgs[1],
                                oldName = betterArgs[2],
                                newName = betterArgs[3],
                            )
                        } else {
                            //invalid usage
                            realPlayer.sendMessage(
                                getStringFromConfig("usage.renameclaim")
                            )
                            return@launch
                        }
                    }

                    5 -> {
                        // /renameclaim -p <player> <old-claim-name> <new-claim-name> confirm
                        if (betterArgs[0] == "-p") {
                            //renaming other player's claim with possible merge confirmation
                            preService.renameForeignClaim(
                                context = context,
                                targetPlayerName = betterArgs[1],
                                oldName = betterArgs[2],
                                newName = betterArgs[3],
                                confirmMerge = betterArgs[4].lowercase() == getStringFromConfig("trigger-words.renameclaim-confirm").lowercase()
                            )
                        } else {
                            //invalid usage
                            realPlayer.sendMessage(
                                getStringFromConfig("usage.renameclaim")
                            )
                            return@launch
                        }
                    }

                    else -> {
                        //invalid usage
                        realPlayer.sendMessage(
                            getStringFromConfig("usage.renameclaim")
                        )
                        return@launch
                    }
                }

                when (result) {
                    is VCResult.RenameClaim.RenamedSuccessful -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.renameclaim.success")
                                .replace("<claim-name>", result.oldName)
                                .replace("<claim-new-name>", result.newName)
                        )
                    }

                    is VCResult.RenameClaim.MergeSuccessful -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.renameclaim.merge-success")
                                .replace("<claim-name>", result.oldName)
                                .replace("<claim-new-name>", result.newName)
                        )
                    }

                    is VCResult.RenameClaim.OldNameNotFound -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.renameclaim.not-found")
                                .replace("<claim-name>", result.oldName)
                        )
                    }

                    is VCResult.RenameClaim.ConfirmMergeRequired -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.renameclaim.merge-confirm")
                                .replace("<claim-name>", "\"${result.oldName}\"")
                                .replace("<claim-new-name>", "\"${result.newName}\"")
                                .replace(
                                    "<renameclaim-confirm>",
                                    getStringFromConfig("trigger-words.renameclaim-confirm")
                                )
                        )
                    }

                    is VCResult.RenameClaim.ConfirmMergeOtherPlayerClaimRequired -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.renameclaim.merge-other-confirm")
                                .replace("<claim-name>", "\"${result.oldName}\"")
                                .replace("<claim-new-name>", "\"${result.newName}\"")
                                .replace(
                                    "<renameclaim-confirm>",
                                    getStringFromConfig("trigger-words.renameclaim-confirm")
                                )
                        )
                    }

                    is VCResult.UnknownFailure -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.unknown-error")
                        )
                    }

                    is VCResult.MissingPermission -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.missing-permission")
                        )
                    }

                    is VCResult.RenameClaim.ClaimNameNotAllowed -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claim.claim-name-not-allowed")
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
                    if(context.restrictions.renameOtherPlayerClaims){
                        names.add("-p")
                    }
                    return names
                }

                2 -> {
                    if (betterArgs[0] == "-p" && context.restrictions.renameOtherPlayerClaims) {
                        //player names
                        val playerNames = preService.getCachedPlayerNames().map { it }
                        return playerNames.toMutableList()
                    } else {
                        val names = context.claims.map { "\"" + it.displayName + "\"" }.toMutableList()
                        if(context.restrictions.renameOtherPlayerClaims){
                            names.add("-p")
                        }
                        names.removeIf { it == "\"${betterArgs[0]}\"" }
                        return names
                    }
                }

                3 -> {
                    if (betterArgs[0] == "-p" && context.restrictions.renameOtherPlayerClaims) {
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
