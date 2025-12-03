package dev.ewio.claim.command

import dev.ewio.claim.service.MessageService
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.getCorrectlySplitArgs
import kotlinx.coroutines.CoroutineScope
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
    private val ms: MessageService
): TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
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

            if(betterArgs.size == 1){
                val partialClaimName = betterArgs[0].lowercase()
                val matchingClaimNames = context.claims.map {
                    it.displayName
                }.filter {
                    it.lowercase().startsWith(partialClaimName)
                }
                return matchingClaimNames.toMutableList()
            }
            if(betterArgs.size == 2){
                val colors = listOf("RED", "GREEN", "BLUE", "YELLOW", "PURPLE", "ORANGE", "WHITE", "BLACK")
                val partialColor = betterArgs[1].lowercase()
                val matchingColors = colors.filter {
                    it.lowercase().startsWith(partialColor)
                }
                return matchingColors.toMutableList()
            }
        }

        return mutableListOf()
    }
}