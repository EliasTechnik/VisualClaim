package dev.ewio.claim.command

import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.service.ColorService
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
 * Command to change the color of a claim.
 *  /claimcolor <claimName> <color>
 */


class ClaimcolorCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val ms: MessageService,
    private val colorService: ColorService
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
                var (context, realPlayer) = it

                val result = when(betterArgs.size){
                    2 -> {
                        // /claimcolor <claimName> <color>
                        if(betterArgs[0].equals("-p", ignoreCase = true)) {
                            VCResult.MalformedCommand
                        }

                        preService.changeClaimColor(
                            context = context,
                            claimName = betterArgs[0],
                            colorName = betterArgs[1]
                        )
                    }
                    4 -> {
                        // /claimcolor -p <playerName> <claimName> <color>

                        if(betterArgs[0].equals("-p", ignoreCase = true)) {
                           preService.changeOtherPlayersClaimColor(
                               context = context,
                               targetPlayerName = betterArgs[1],
                               claimName = betterArgs[2],
                               colorName = betterArgs[3]
                           )
                        } else {
                            VCResult.MalformedCommand
                        }
                    }
                    else -> {
                        VCResult.MalformedCommand
                    }
                }

                when(result){
                    is VCResult.ClaimColor.ColorSet -> {
                        ms.send(
                            player = realPlayer,
                            key = "claimcolor.set",
                            placeholders = mapOf(
                                "claim_name" to result.claim.displayName,
                                "color_name" to result.color.name
                            )
                        )
                    }
                    is VCResult.VCClaimNotFound -> {
                        ms.send(
                            player = realPlayer,
                            key = "claim-not-found",
                            placeholders = mapOf(
                                "claim_name" to result.claimName
                            )
                        )
                    }
                    is VCResult.ClaimColor.ColorNotFound -> {
                        ms.send(
                            player = realPlayer,
                            key = "claimcolor.invalid-color"
                        )
                    }
                    is VCResult.MalformedCommand -> {
                        ms.send(
                            player = realPlayer,
                            key = "usage.claimcolor"
                        )
                    }
                    is VCResult.VCPlayerNotFound -> {
                        ms.send(
                            player = realPlayer,
                            key = "player-not-found",
                            placeholders = mapOf(
                                "player_name" to result.playerName
                            )
                        )
                    }
                    is VCResult.MissingPermission -> {
                        ms.send(
                            player = realPlayer,
                            key = "missing-permission"
                        )
                    }
                    else -> {
                        ms.send(
                            player = realPlayer,
                            key = "unknown-error"
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
    ): MutableList<String>? {
        val betterArgs = getCorrectlySplitArgs(args.toList(),0)
        val player = sender as? Player?: return mutableListOf()

        preService.getCachedPlayerContext(player)?.let { context ->

            when(betterArgs.size){
                1 -> {
                    val partialClaimName = betterArgs[0].lowercase()
                    val matchingClaimNames = context.claims.map {
                        "\"${it.displayName}\""
                    }.filter {
                        it.lowercase().startsWith(partialClaimName)
                    }.toMutableList()
                    if(context.restrictions.claimColorOther) matchingClaimNames.add("-p")
                    return matchingClaimNames
                }
                2 -> {
                    if(betterArgs[0].equals("-p", ignoreCase = true) && context.restrictions.claimColorOther){
                        val partialPlayerName = betterArgs[1].lowercase()
                        val matchingPlayerNames = preService.getCachedPlayerNames().filter {
                            it.lowercase().startsWith(partialPlayerName)
                        }
                        return matchingPlayerNames.toMutableList()
                    } else {
                        val colors = colorService.colorsList
                        val partialColor = betterArgs[1].lowercase()
                        val matchingColors = colors.filter {
                            it.name.lowercase().startsWith(partialColor)
                        }.map { it.name }
                        return matchingColors.toMutableList()
                    }
                }
                3 -> {
                    if(betterArgs[0].equals("-p", ignoreCase = true) && context.restrictions.claimColorOther){
                        val partialClaimName = betterArgs[2].lowercase()
                        val matchingClaimNames = context.claims.map {
                            it.displayName
                        }.filter {
                            it.lowercase().startsWith(partialClaimName)
                        }
                        return matchingClaimNames.toMutableList()
                    }
                 }
            }
        }

        return mutableListOf()
    }
}