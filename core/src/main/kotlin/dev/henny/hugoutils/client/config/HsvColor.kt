package dev.henny.hugoutils.client.config

/** Gemeinsame HSV-Farbschnittstelle, damit der ColorPicker jeden Style bearbeiten kann. */
interface HsvColor {
    var hue: Float
    var saturation: Float
    var brightness: Float

    fun hex(): String
    fun setFromHex(hex: String): Boolean
}
