package dev.ewio.claim.service

import dev.ewio.util.log
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import kotlin.collections.iterator

class MessageService(
    private val config: FileConfiguration
) {

    private val mm = MiniMessage.miniMessage()
    private val templates = HashMap<String, String>()

    fun load() {
        val section = config.getConfigurationSection("messages") ?: return
        for (key in section.getKeys(true)) {
            val fullKey = "messages.$key"
            val value = config.getString(fullKey) ?: continue
            templates[key] = replaceLegacyFormattingCodes(value)
            //log("Loaded message template: $key -> $value")
        }
    }

    /** Liefert Component; ersetzt Platzhalter in der Form %name% */
    fun format(key: String, placeholders: Map<String, String> = emptyMap()): Component {
        val template = templates[key] ?: return mm.deserialize("<red>Missing message: $key")
        var s = template
        for ((k, v) in placeholders) {
            s = s.replace("%$k%", v)
        }
        return mm.deserialize(s)
    }

    fun send(player: Player, key: String, placeholders: Map<String, String> = emptyMap()) {
        val comp = format(key, placeholders)
        // wenn du BukkitAudiences benutzt, kannst du audiences.player(player).sendMessage(comp)
        player.sendMessage(comp) // Paper unterstützt Component direkt
    }

    fun getString(key: String, placeholders: Map<String, String> = emptyMap()): String {
        val template = templates[key] ?: return "Missing message: $key"
        var s = template
        for ((k, v) in placeholders) {
            s = s.replace("%$k%", v)
        }
        return s
    }

    fun getEmptyComponent(): Component {
        return mm.deserialize("")
    }

    private fun replaceLegacyFormattingCodes(input: String): String {
        return input
            .replace("§0", "<black>")
            .replace("§1", "<dark_blue>")
            .replace("§2", "<dark_green>")
            .replace("§3", "<dark_aqua>")
            .replace("§4", "<dark_red>")
            .replace("§5", "<dark_purple>")
            .replace("§6", "<gold>")
            .replace("§7", "<gray>")
            .replace("§8", "<dark_gray>")
            .replace("§9", "<blue>")
            .replace("§a", "<green>")
            .replace("§b", "<aqua>")
            .replace("§c", "<red>")
            .replace("§d", "<light_purple>")
            .replace("§e", "<yellow>")
            .replace("§f", "<white>")
            .replace("§l", "<bold>")
            .replace("§m", "<strikethrough>")
            .replace("§n", "<underlined>")
            .replace("§o", "<italic>")
            .replace("§r", "<reset>")
            .replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>")
            .replace("&o", "<italic>")
            .replace("&r", "<reset>")
    }

    /*
    fun getProcessedString(
        chunkX: Int?,
        chunkZ: Int?,
        claimName: String?,
        claimNewName: String?,
        maxChunks: Int?,
        maxClaims: Int?,
        maxNameLength: Int?,
        owner: String?,
        other: String?, //other player's name
        chunkCount: Int?,
        claimCount: Int?,
        deleteClaimCofirm: String?,
        renameClaimConfirm: String?,
        )
    */
}