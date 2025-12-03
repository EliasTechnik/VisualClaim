package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCColor


class ColorService(
    private val getStringFromConfig: (key: String) -> String?
){
    var colors: Map<String, VCColor>

    init {
        // Load colors from configuration
    }
}