package dev.henny.hugoutils.client.config

import com.google.gson.JsonObject

interface ConfigSection {
    val id: String
    fun read(json: JsonObject)
    fun write(): JsonObject
}
