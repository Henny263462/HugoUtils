package dev.henny.hugoutils

import net.minecraft.util.Identifier

object HugoIds {
    const val MOD_ID = "hugoutils"
    const val DISPLAY_NAME = "HugoSMPUtils"

    fun id(path: String): Identifier = Identifier.of(MOD_ID, path)
}
