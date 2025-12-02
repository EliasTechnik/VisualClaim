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

class ShowClaimCommand(
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
                var (context, realPlayer) = it

                if(betterArgs.size == 1){
                    //execute showclaim
                    val claimName = betterArgs[0]
                    preService.showClaim(
                        context = context,
                        claimString = claimName,
                        player = realPlayer
                    )
                    return@launch
                }else {
                    //invalid usage
                    ms.send(realPlayer, "usage.showclaim")
                    return@launch
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

        preService.getCachedPlayerContext(player)?.let{ context ->
            //get available claims
            val names = context.claims.map {"\"" + it.displayName +"\"" }

            if (names.isEmpty()) {
                return mutableListOf()
            }

            return when (betterArgs.size) {
                1 -> {
                    names.toMutableList()
                }
                else -> mutableListOf()
            }
        }
        return mutableListOf()
    }


}