package dev.henny.hugoutils.client.ui

import com.google.gson.JsonObject
import dev.henny.hugoutils.api.JsonView
import net.minecraft.client.MinecraftClient
import net.minecraft.stat.Stats
import java.util.Locale

data class PlayerStatChip(
    val label: String,
    val value: String
)

object LocalPlayerStats {
    fun chips(api: JsonObject? = null): List<PlayerStatChip> {
        val client = MinecraftClient.getInstance()
        val player = client.player
        val stats = player?.statHandler
        val playTicks = stat(stats, Stats.PLAY_TIME) ?: ticksFromApi(api, "playtime", "playTime", "play_time", "stats.playtime")
        val deaths = stat(stats, Stats.DEATHS) ?: numberFromApi(api, "deaths", "stats.deaths")
        val kills = stat(stats, Stats.MOB_KILLS) ?: numberFromApi(api, "kills", "mobKills", "stats.kills")
        val jumps = stat(stats, Stats.JUMP) ?: numberFromApi(api, "jumps", "stats.jumps")
        val walked = stat(stats, Stats.WALK_ONE_CM) ?: numberFromApi(api, "walked", "stats.walked")
        val session = player?.age?.toLong()
        return listOf(
            PlayerStatChip("Spielzeit", formatDuration(playTicks)),
            PlayerStatChip("Session", formatDuration(session)),
            PlayerStatChip("Tode", formatCount(deaths)),
            PlayerStatChip("Kills", formatCount(kills)),
            PlayerStatChip("Sprünge", formatCount(jumps)),
            PlayerStatChip("Gelaufen", formatDistance(walked))
        )
    }

    private fun stat(handler: net.minecraft.stat.StatHandler?, id: net.minecraft.util.Identifier): Long? {
        if (handler == null) return null
        return runCatching { handler.getStat(Stats.CUSTOM.getOrCreateStat(id)).toLong() }.getOrNull()
            ?.takeIf { it > 0L }
    }

    private fun ticksFromApi(api: JsonObject?, vararg keys: String): Long? {
        val seconds = JsonView.number(api, *keys) ?: return null
        return (seconds * 20.0).toLong().takeIf { it > 0L }
    }

    private fun numberFromApi(api: JsonObject?, vararg keys: String): Long? =
        JsonView.number(api, *keys)?.toLong()?.takeIf { it > 0L }

    private fun formatCount(value: Long?): String =
        if (value == null || value <= 0L) "—" else String.format(Locale.GERMANY, "%,d", value)

    private fun formatDuration(ticks: Long?): String {
        if (ticks == null || ticks <= 0L) return "—"
        val totalMinutes = ticks / 20L / 60L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        val days = hours / 24L
        val restHours = hours % 24L
        return when {
            days > 0L -> "${days}d ${restHours}h"
            hours > 0L -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    private fun formatDistance(centimeters: Long?): String {
        if (centimeters == null || centimeters <= 0L) return "—"
        val meters = centimeters / 100.0
        return if (meters >= 1000) String.format(Locale.GERMANY, "%.1f km", meters / 1000.0)
        else String.format(Locale.GERMANY, "%.0f m", meters)
    }
}
