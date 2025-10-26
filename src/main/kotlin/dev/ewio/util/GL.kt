package dev.ewio.util

import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

object GL {
    lateinit var logger: Logger

    fun init(plugin: JavaPlugin) {
        logger = plugin.logger
    }
}