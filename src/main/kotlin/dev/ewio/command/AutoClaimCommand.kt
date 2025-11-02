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

class AutoclaimCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val getStringFromConfig: (key: String) -> String
): TabExecutor {

    /**
     *  /autoclaim <claim-name>
     *  /autoclaim off
     *
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
                val (context, realPlayer) = it

                val result = when (betterArgs.size) {
                    0 -> {
                        VCResult.AutoClaim.StatusInfo(context.player.autoClaim)
                    }
                    1 -> {
                        // /autoclaim <claim-name> or  /autoclaim off
                        if(betterArgs[0].equals(getStringFromConfig("trigger-words.autoclaim-off"), ignoreCase = true)) {
                            //turn off autoclaim
                            preService.disableAutoclaim(context)
                        }else{
                            //might be a claim name
                            preService.enableAutoclaim(context, betterArgs[0])
                        }
                    }

                    else -> {
                        VCResult.MalformedCommand
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

        val betterArgs = getCorrectlySplitArgs(args.toList(), 0)
        val player = sender as? Player ?: return mutableListOf()

        preService.getCachedPlayerContext(player)?.let { context ->
            when (betterArgs.size) {
                1 -> {
                    //claim names
                    val names = context.claims.map { "\"" + it.displayName + "\"" }.toMutableList()
                    names.add(getStringFromConfig("trigger-words.autoclaim-off"))
                    return names
                }
            }
        }

        return mutableListOf()
    }
}