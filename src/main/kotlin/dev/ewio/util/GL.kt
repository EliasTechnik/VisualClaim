package dev.ewio.util

import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

enum class LogLevel {
    INFO, //show all logs
    WARNING, //show severe and warning logs
    SEVERE, //show only severe logs
}

object GL {
    lateinit var logger: Logger

    var level: LogLevel = LogLevel.INFO

    fun init(plugin: JavaPlugin) {
        logger = plugin.logger
    }
}