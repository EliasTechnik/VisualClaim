package dev.ewio.command

import dev.ewio.claim.service.MessageService
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.getCorrectlySplitArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

/**
 *  /chunkloader [add/remove/list]
 */


class ChunkloaderCommand(
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

                if(betterArgs.size == 0){
                    ms.send(realPlayer, "usage.chunkloader")
                    return@launch
                }else{
                    val result = when(betterArgs[0]){
                        "add" -> {
                            if(betterArgs.size >= 2){

                            }else{
                                ms.send(realPlayer, "usage.chunkloader")
                                return@launch
                            }
                        }
                        "remove" -> {
                            if(betterArgs.size >= 2){

                            }else{
                                ms.send(realPlayer, "usage.chunkloader")
                                return@launch
                            }

                        }
                        "list" -> {
                            if(betterArgs.size >= 2){

                            }else{
                                preService.listChunkloader(context, )
                            }

                        }
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
    return mutableListOf()
    }
}