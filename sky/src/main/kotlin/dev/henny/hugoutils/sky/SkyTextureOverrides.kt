package dev.henny.hugoutils.sky

import java.io.ByteArrayInputStream

object SkyTextureOverrides {
    @Volatile
    private var clouds: ByteArray? = null

    fun setClouds(bytes: ByteArray?) {
        clouds = bytes
    }

    @JvmStatic
    fun cloudStreamOrNull(): ByteArrayInputStream? = clouds?.let(::ByteArrayInputStream)
}
