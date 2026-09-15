package dev.henny.hugoutils.client.effects

import dev.henny.hugoutils.client.config.EffectsConfig
import dev.henny.hugoutils.mixin.EffectsParticleAlphaAccessor
import net.minecraft.client.particle.Particle
import net.minecraft.particle.ParticleEffect
import net.minecraft.registry.Registries
import java.util.concurrent.ThreadLocalRandom

object EffectsController {
    @JvmStatic
    fun shouldCreateParticle(effect: ParticleEffect): Boolean {
        val settings = EffectsConfig.settingsFor(particleTypeId(effect))
        if (!settings.enabled || settings.density <= 0f) return false
        return settings.density >= 1f || ThreadLocalRandom.current().nextFloat() < settings.density
    }

    @JvmStatic
    fun applyParticleSettings(particle: Particle, effect: ParticleEffect) {
        val settings = EffectsConfig.settingsFor(particleTypeId(effect))
        if (settings.size != 1f) particle.scale(settings.size)
        if (settings.opacity != 1f && particle is EffectsParticleAlphaAccessor) {
            val accessor = particle
            accessor.`hugoutils$setEffectsAlpha`(
                (accessor.`hugoutils$getEffectsAlpha`() * settings.opacity).coerceIn(0f, 1f)
            )
        }
    }

    @JvmStatic
    fun fireScale(): Float = EffectsConfig.fireScale

    @JvmStatic
    fun nauseaWobbleScale(): Float = EffectsConfig.nauseaWobbleScale

    private fun particleTypeId(effect: ParticleEffect): String =
        Registries.PARTICLE_TYPE.getId(effect.type).toString()
}
