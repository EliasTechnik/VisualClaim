package dev.ewio.claim.command

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