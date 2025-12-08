package dev.ewio.claim.definitions

import dev.ewio.claim.service.ColorService
import net.kyori.adventure.bossbar.BossBar

enum class VCColorType(val identifier: String) {
    RED("red"),
    BLUE("blue"),
    GREEN("green"),
    YELLOW("yellow"),
    PURPLE("purple"),
    ORANGE("orange"),
    TURQUOISE("turquoise"),
    WHITE("white"),
    BLACK("black"),
    GRAY("gray"),
    PINK("pink"),
    CYAN("cyan"),
    LIME("lime"),
    LIGHT_BLUE("light_blue"),
    MAGENTA("magenta"),
    BROWN("brown"),
    GOLD("gold");

    companion object {
        fun getRandomColorType(): VCColorType {
            return values().random()
        }

        fun getByIdentifier(string: String): VCColorType {
            return values().firstOrNull { it.identifier.equals(string, ignoreCase = true) }
                ?: VCColorType.WHITE
        }
    }
}

data class VCColor(
    val hex: String,
    val name: String,
    val type: VCColorType
){
    override fun toString(): String {
        return "VCColor(type='$type' hex='$hex', name='$name')"
    }

    fun getIntColor(): Int {
        return Integer.parseInt(hex.removePrefix("#"), 16)
    }

    fun getIntColorWithAlpha(alpha: Int): Int {
        val rgb = getIntColor()
        return (alpha and 0xFF) shl 24 or (rgb and 0xFFFFFF)
    }

}

object VCColorLookup{
    private var colorService: ColorService? = null

    fun initialize(service: ColorService){
        colorService = service
    }

    fun colorFromIdentifier(identifier: String): VCColor {
        val type = VCColorType.getByIdentifier(identifier)
        return colorService?.colors?.get(type) ?: VCColor(
            hex = "#FFFFFF",
            name = "White",
            type = VCColorType.WHITE
        )
    }
}
