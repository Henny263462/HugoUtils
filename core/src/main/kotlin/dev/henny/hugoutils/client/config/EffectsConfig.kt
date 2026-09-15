package dev.henny.hugoutils.client.config

import com.google.gson.JsonObject

data class ParticleTypeOverride(
    var enabled: Boolean = true,
    var density: Float = 1f,
    var size: Float = 1f,
    var opacity: Float = 1f
) {
    fun clamp() {
        density = density.coerceIn(0f, 1f)
        size = size.coerceIn(0f, 3f)
        opacity = opacity.coerceIn(0f, 1f)
    }
}

object EffectsConfig : ConfigSection {
    override val id: String = "effects"

    var particlesEnabled: Boolean = true
    var particleDensity: Float = 1f
    var particleSize: Float = 1f
    var particleOpacity: Float = 1f

    /** Scale factors. Zero disables the corresponding effect; one is vanilla. */
    var fireScale: Float = 1f
    var portalScale: Float = 1f
    var nauseaScale: Float = 1f
    var nauseaWobbleScale: Float = 1f

    val particleOverrides: MutableMap<String, ParticleTypeOverride> = linkedMapOf()

    fun reset() {
        particlesEnabled = true
        particleDensity = 1f
        particleSize = 1f
        particleOpacity = 1f
        fireScale = 1f
        portalScale = 1f
        nauseaScale = 1f
        nauseaWobbleScale = 1f
        particleOverrides.clear()
    }

    fun clamp() {
        particleDensity = particleDensity.coerceIn(0f, 1f)
        particleSize = particleSize.coerceIn(0f, 3f)
        particleOpacity = particleOpacity.coerceIn(0f, 1f)
        fireScale = fireScale.coerceIn(0f, 1f)
        portalScale = portalScale.coerceIn(0f, 1f)
        nauseaScale = nauseaScale.coerceIn(0f, 1f)
        nauseaWobbleScale = nauseaWobbleScale.coerceIn(0f, 1f)
        particleOverrides.values.forEach(ParticleTypeOverride::clamp)
    }

    fun settingsFor(typeId: String): ParticleTypeOverride {
        val override = particleOverrides[typeId]
        return ParticleTypeOverride(
            enabled = particlesEnabled && (override?.enabled ?: true),
            density = particleDensity * (override?.density ?: 1f),
            size = particleSize * (override?.size ?: 1f),
            opacity = particleOpacity * (override?.opacity ?: 1f)
        ).also(ParticleTypeOverride::clamp)
    }

    override fun read(json: JsonObject) {
        reset()
        particlesEnabled = json.boolean("particlesEnabled", true)
        particleDensity = json.float("particleDensity", 1f)
        particleSize = json.float("particleSize", 1f)
        particleOpacity = json.float("particleOpacity", 1f)
        fireScale = json.float("fireScale", 1f)
        portalScale = json.float("portalScale", 1f)
        nauseaScale = json.float("nauseaScale", 1f)
        nauseaWobbleScale = json.float("nauseaWobbleScale", 1f)
        json.getAsJsonObject("particleOverrides")?.entrySet()?.forEach { (id, value) ->
            if (value.isJsonObject) {
                val entry = value.asJsonObject
                particleOverrides[id] = ParticleTypeOverride(
                    enabled = entry.boolean("enabled", true),
                    density = entry.float("density", 1f),
                    size = entry.float("size", 1f),
                    opacity = entry.float("opacity", 1f)
                )
            }
        }
        clamp()
    }

    override fun write(): JsonObject {
        clamp()
        return JsonObject().apply {
            addProperty("particlesEnabled", particlesEnabled)
            addProperty("particleDensity", particleDensity)
            addProperty("particleSize", particleSize)
            addProperty("particleOpacity", particleOpacity)
            addProperty("fireScale", fireScale)
            addProperty("portalScale", portalScale)
            addProperty("nauseaScale", nauseaScale)
            addProperty("nauseaWobbleScale", nauseaWobbleScale)
            add("particleOverrides", JsonObject().also { overrides ->
                particleOverrides.toSortedMap().forEach { (id, value) ->
                    overrides.add(id, JsonObject().apply {
                        addProperty("enabled", value.enabled)
                        addProperty("density", value.density)
                        addProperty("size", value.size)
                        addProperty("opacity", value.opacity)
                    })
                }
            })
        }
    }

    private fun JsonObject.boolean(name: String, fallback: Boolean): Boolean =
        runCatching { get(name)?.asBoolean }.getOrNull() ?: fallback

    private fun JsonObject.float(name: String, fallback: Float): Float =
        runCatching { get(name)?.asFloat }.getOrNull() ?: fallback
}
