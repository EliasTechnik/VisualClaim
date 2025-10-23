package dev.ewio.util

import java.awt.Color

enum class VCColorType(id: String){
    red("red"),
    blue("blue"),
    green("green"),
    yellow("yellow"),
    purple("purple"),
    orange("orange"),
    white("white"),
    black("black"),
    gray("gray"),
    pink("pink"),
    cyan("cyan"),
    lime("lime"),
    light_blue("light_blue"),
    magenta("magenta"),
    brown("brown"),
    gold("gold"),
    silver("silver")
}

data class VCColor(
    val hexValue: String,
    val type: VCColorType,
    val name: String
){
    fun toColor(): Color{
        return Color.decode(hexValue)
    }

    fun toInt(): Int {
        return Color.decode(hexValue).rgb
    }
}

class ColorHelper {
}