package dev.ewio.claim.definitions

import net.kyori.adventure.bossbar.BossBar

data class VCColor(
    val hex: String,
    val name: String
){
    override fun toString(): String {
        return "VCColor(hex='$hex', name='$name')"
    }

    fun toBossBarColor(): BossBar.Color {
        return BossBar.Color.valueOf(hex)
    }

    companion object {
        fun randomColor(): VCColor {
            val colors = listOf(
                VCColor("#FF0000", "Red"),
                VCColor("#00FF00", "Green"),
                VCColor("#0000FF", "Blue"),
                VCColor("#FFFF00", "Yellow"),
                VCColor("#FF00FF", "Purple"),
                VCColor("#FFA500", "Orange"),
                VCColor("##ADD8E6", "Turquoise"),
                VCColor("#E74C3C", "Crimson"),
                VCColor("#2ECC71", "Emerald"),
                VCColor("#3498DB", "Sky Blue")
            )
            return colors.random()
        }
    }
}
