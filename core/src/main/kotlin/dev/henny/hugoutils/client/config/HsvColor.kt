package dev.henny.hugoutils.client.config

interface HsvColor : dev.henny.hugoutils.ui.HsvColor {

    fun hex(): String
    fun setFromHex(hex: String): Boolean
}
