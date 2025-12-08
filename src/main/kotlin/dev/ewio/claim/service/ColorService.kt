package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCColor
import dev.ewio.claim.definitions.VCColorLookup
import dev.ewio.claim.definitions.VCColorType
import dev.ewio.util.log


class ColorService(
    private val getStringFromConfig: (key: String) -> String?
){
    private var colorMap: Map<VCColorType, VCColor> = mutableMapOf()

    val colors: Map<VCColorType, VCColor>
        get() = colorMap

    val colorsList: List<VCColor>
        get() = colorMap.values.toList()

    init {
        // Load colors from configuration
        val colorsToLoad = VCColorType.entries.toTypedArray()

        val tempMap = mutableMapOf<VCColorType, VCColor>()

        colorsToLoad.forEach {
            val hex = getStringFromConfig("color.values.${it.identifier}") ?: "#FFFFFF"
            val name = getStringFromConfig("color.names.${it.identifier}") ?: "White"
            val color = VCColor(
                hex = hex,
                name = name,
                type = it
            )
            tempMap[it] = color
            log("Loaded color: $color")
        }

        colorMap = tempMap
        log("Total colors loaded: ${colorMap.size}")

        //register lookup
        VCColorLookup.initialize(this)
    }

    fun getRandomColor(): VCColor {
        return colors.values.random()
    }
}