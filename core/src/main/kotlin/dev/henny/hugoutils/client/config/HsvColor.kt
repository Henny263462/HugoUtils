package dev.henny.hugoutils.client.config

interface HsvColor {
    var hue: Float
    var saturation: Float
    var brightness: Float

    fun hex(): String
    fun setFromHex(hex: String): Boolean
}
