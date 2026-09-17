package dev.henny.hugoutils.client.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import dev.henny.hugoutils.HugoIds
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object ConfigManager {
    private val logger = LoggerFactory.getLogger(HugoIds.DISPLAY_NAME)
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val configDir: Path = FabricLoader.getInstance().configDir
    private val rootDir: Path = configDir.resolve(HugoIds.MOD_ID)
    private val settingsPath: Path = rootDir.resolve("settings.json")
    private val profilesDir: Path = rootDir.resolve("profiles")
    private val legacyCorePath: Path = configDir.resolve("${HugoIds.MOD_ID}.json")
    private val sections = LinkedHashMap<String, ConfigSection>()
    private val pendingSections = LinkedHashMap<String, JsonObject>()
    private val saveExecutor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "HugoUtils Config Save").apply { isDaemon = true }
    }
    private var pendingSave: ScheduledFuture<*>? = null
    private var loaded = false

    val config: ModConfig = ModConfig()
    var activeProfile: String? = null
        private set

    @Synchronized
    fun load() {
        val source = if (Files.exists(settingsPath)) settingsPath else legacyCorePath
        if (!Files.exists(source)) {
            loaded = true
            applyUiMetrics()
            save()
            return
        }
        try {
            Files.newBufferedReader(source).use { reader ->
                val tree = JsonParser.parseReader(reader)
                if (tree is JsonObject) {
                    if (tree.has("core")) {
                        applyRoot(tree)
                    } else {
                        applyCore(tree)
                    }
                }
            }
        } catch (exception: JsonSyntaxException) {
            logger.warn("Konnte {} nicht lesen, verwende Standardwerte.", source.fileName, exception)
        } catch (exception: Exception) {
            logger.warn("Konnte Config nicht laden.", exception)
        }
        loaded = true
        applyUiMetrics()
        save()
    }

    @Synchronized
    fun save() {
        config.clamp()
        try {
            writeAtomic(settingsPath, snapshot())
        } catch (exception: Exception) {
            logger.warn("Konnte Config nicht speichern.", exception)
        }
    }

    @Synchronized
    fun requestSave() {
        pendingSave?.cancel(false)
        pendingSave = saveExecutor.schedule({ save() }, 250, TimeUnit.MILLISECONDS)
    }

    fun update(mutator: (ModConfig) -> Unit) {
        mutator(config)
        config.clamp()
        applyUiMetrics()
        save()
    }

    fun pinnedMarketItems(): List<String> = config.pinnedMarketItems.toList()

    fun isPinnedMarketItem(id: String): Boolean = config.pinnedMarketItems.contains(id)

    fun togglePinnedMarketItem(id: String) {
        val next = ArrayList(config.pinnedMarketItems)
        if (!next.remove(id)) next.add(0, id)
        config.pinnedMarketItems = next
        requestSave()
    }

    fun priceAlerts(): List<PriceAlert> = config.priceAlerts.toList()

    fun upsertPriceAlert(alert: PriceAlert) {
        if (alert.itemId.isBlank() || alert.target <= 0.0) return
        val next = ArrayList(config.priceAlerts.filter { it.itemId != alert.itemId })
        next.add(0, alert)
        config.priceAlerts = next
        requestSave()
    }

    fun removePriceAlert(itemId: String) {
        config.priceAlerts = ArrayList(config.priceAlerts.filter { it.itemId != itemId })
        requestSave()
    }

    fun applyUiMetrics() {
        dev.henny.hugoutils.ui.UiMetrics.corner = config.uiCornerRadius
    }

    fun setUiCornerRadius(value: Int) {
        config.uiCornerRadius = value.coerceIn(0, 12)
        applyUiMetrics()
        requestSave()
    }

    @Synchronized
    fun moduleData(id: String): JsonObject? = sections[id]?.write() ?: pendingSections[id]

    @Synchronized
    fun registerSection(section: ConfigSection, legacyPath: Path? = null) {
        sections[section.id] = section
        val pending = pendingSections.remove(section.id)
        when {
            pending != null -> section.read(pending)
            legacyPath != null && Files.exists(legacyPath) -> runCatching {
                Files.newBufferedReader(legacyPath).use {
                    val parsed = JsonParser.parseReader(it)
                    if (parsed is JsonObject) section.read(parsed)
                }
            }.onFailure { logger.warn("Konnte Legacy-Sektion {} nicht importieren.", section.id, it) }
        }
        if (loaded) save()
    }

    @Synchronized
    fun profileNames(): List<String> {
        ensureInitialProfile()
        if (!Files.exists(profilesDir)) return emptyList()
        return Files.list(profilesDir).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .map { it.fileName.toString().removeSuffix(".json") }
                .sorted()
                .toList()
        }
    }

    @Synchronized
    fun saveProfile(name: String): Boolean {
        val safe = safeProfileName(name) ?: return false
        return runCatching {
            writeAtomic(profilesDir.resolve("$safe.json"), snapshot())
            activeProfile = safe
            save()
        }.onFailure { logger.warn("Konnte Profil {} nicht speichern.", safe, it) }.isSuccess
    }

    @Synchronized
    fun loadProfile(name: String): Boolean {
        val safe = safeProfileName(name) ?: return false
        val path = profilesDir.resolve("$safe.json")
        if (!Files.exists(path)) return false
        return runCatching {
            Files.newBufferedReader(path).use { reader ->
                val parsed = JsonParser.parseReader(reader)
                require(parsed is JsonObject)
                applyRoot(parsed)
            }
            activeProfile = safe
            GlintChangeListener.fire()
            save()
        }.onFailure { logger.warn("Konnte Profil {} nicht laden.", safe, it) }.isSuccess
    }

    @Synchronized
    fun deleteProfile(name: String): Boolean {
        val safe = safeProfileName(name) ?: return false
        return runCatching {
            val deleted = Files.deleteIfExists(profilesDir.resolve("$safe.json"))
            if (deleted && activeProfile == safe) {
                activeProfile = null
                save()
            }
            deleted
        }.getOrDefault(false)
    }

    @Synchronized
    fun renameProfile(from: String, to: String): Boolean {
        val oldName = safeProfileName(from) ?: return false
        val newName = safeProfileName(to) ?: return false
        val source = profilesDir.resolve("$oldName.json")
        val target = profilesDir.resolve("$newName.json")
        if (!Files.exists(source) || Files.exists(target)) return false
        return runCatching {
            Files.move(source, target)
            if (activeProfile == oldName) activeProfile = newName
            save()
        }.isSuccess
    }

    private fun snapshot(): JsonObject = JsonObject().apply {
        addProperty("version", 2)
        activeProfile?.let { addProperty("activeProfile", it) }
        add("core", gson.toJsonTree(config))
        val moduleJson = JsonObject()
        pendingSections.forEach(moduleJson::add)
        sections.values.forEach { moduleJson.add(it.id, it.write()) }
        add("modules", moduleJson)
    }

    private fun applyRoot(root: JsonObject) {
        activeProfile = root.get("activeProfile")?.takeIf(JsonElement::isJsonPrimitive)?.asString
        root.getAsJsonObject("core")?.let(::applyCore)
        root.getAsJsonObject("modules")?.entrySet()?.forEach { (id, value) ->
            if (value is JsonObject) {
                sections[id]?.read(value) ?: pendingSections.put(id, value.deepCopy())
            }
        }
    }

    private fun applyCore(tree: JsonObject) {
        val value = gson.fromJson(tree, ModConfig::class.java) ?: return
        value.droppedItemGlow?.let(config.droppedItemGlow::copyFrom)
        value.heldItemGlow?.let(config.heldItemGlow::copyFrom)
        value.playerGlow?.let(config.playerGlow::copyFrom)
        value.heldGlint?.let(config.heldGlint::copyFrom)
        if (tree.has("perspectiveMode")) {
            config.perspectiveMode = value.perspectiveMode
        }
        if (tree.has("checkUpdatesAutomatically")) {
            config.checkUpdatesAutomatically = value.checkUpdatesAutomatically
        }
        if (tree.has("includePrereleases")) {
            config.includePrereleases = value.includePrereleases
        }
        if (tree.has("uiDebugCommandsEnabled")) {
            config.uiDebugCommandsEnabled = value.uiDebugCommandsEnabled
        }
        if (tree.has("lastSettingsPage") && value.lastSettingsPage.isNotBlank()) {
            config.lastSettingsPage = value.lastSettingsPage
        }
        if (tree.has("lastOpenedPage") && value.lastOpenedPage.isNotBlank()) {
            config.lastOpenedPage = value.lastOpenedPage
        }
        if (tree.has("restoreLastPage")) {
            config.restoreLastPage = value.restoreLastPage
        }
        if (tree.has("marketListView")) {
            config.marketListView = value.marketListView
        }
        if (tree.has("pinnedMarketItems")) {
            config.pinnedMarketItems.clear()
            config.pinnedMarketItems.addAll(value.pinnedMarketItems.filter { it.isNotBlank() }.distinct())
        }
        if (tree.has("uiCornerRadius")) {
            config.uiCornerRadius = value.uiCornerRadius
        }
        if (tree.has("priceAlerts")) {
            config.priceAlerts.clear()
            config.priceAlerts.addAll(
                value.priceAlerts.filter { it.itemId.isNotBlank() && it.target > 0.0 }
            )
        }
        applyUiMetrics()
        val dropped = tree.getAsJsonObject("droppedItemGlow")
        val held = tree.getAsJsonObject("heldItemGlow")
        val player = tree.getAsJsonObject("playerGlow")
        migrateGlow(config.droppedItemGlow, dropped)
        migrateGlow(config.heldItemGlow, held)
        migrateGlow(config.playerGlow, player)
        if (!tree.has("playerGlow")) config.playerGlow.copyFrom(GlowStyle.playerDefault())
        migrateLegacyItemFilter(tree)
        if (!tree.has("heldGlint")) {
            config.heldGlint.enabled = config.heldItemGlow.enabled
            config.heldItemGlow.enabled = false
        }
        config.clamp()
    }

    private fun migrateGlow(style: GlowStyle, json: JsonObject?) {
        if (json == null || json.has("opacity")) return
        val transparency = json.get("transparency")?.asFloat ?: 0.3f
        val intensity = json.get("intensity")?.asFloat ?: 0.75f
        val width = json.get("width")?.asFloat ?: 0.5f
        style.opacity = ((1f - transparency) * (0.2f + intensity * 0.8f)).coerceIn(0f, 1f)
        style.thicknessPixels = (1f + width.coerceIn(0f, 1f) * 3f).toInt().coerceIn(1, 4)
    }

    private fun ensureInitialProfile() {
        Files.createDirectories(profilesDir)
        val hasProfiles = Files.list(profilesDir).use { it.anyMatch { path -> path.fileName.toString().endsWith(".json") } }
        if (!hasProfiles) {
            writeAtomic(profilesDir.resolve("pvp.json"), pvpSnapshot())
        }
    }

    private fun pvpSnapshot(): JsonObject {
        val root = snapshot().deepCopy()
        val core = root.getAsJsonObject("core")
        core.getAsJsonObject("droppedItemGlow")?.apply {
            addProperty("enabled", true)
            addProperty("opacity", 0.8f)
        }
        core.getAsJsonObject("playerGlow")?.apply {
            addProperty("enabled", true)
            addProperty("hue", 0f)
            addProperty("saturation", 0.9f)
            addProperty("brightness", 1f)
            addProperty("opacity", 0.85f)
        }
        core.getAsJsonObject("heldGlint")?.apply {
            addProperty("enabled", true)
            addProperty("mode", "strong")
        }
        return root
    }

    private fun safeProfileName(value: String): String? {
        val normalized = value.trim().lowercase().replace(' ', '_')
        return normalized.takeIf { it.matches(Regex("[a-z0-9_-]{1,40}")) }
    }

    private fun writeAtomic(path: Path, json: JsonObject) {
        Files.createDirectories(path.parent)
        val temporary = path.resolveSibling("${path.fileName}.tmp")
        Files.newBufferedWriter(temporary).use { gson.toJson(json, it) }
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun migrateLegacyItemFilter(tree: JsonObject) {
        val dropped = tree.getAsJsonObject("droppedItemGlow")
        if (dropped != null && dropped.has("filter")) {
            return
        }
        if (!tree.has("glowFilterMode") && !tree.has("glowItems")) {
            return
        }
        val legacy = ItemFilter()
        if (tree.has("glowFilterMode")) {
            legacy.mode = tree.get("glowFilterMode").asString
        }
        if (tree.has("glowItems") && tree.get("glowItems").isJsonArray) {
            for (entry in tree.getAsJsonArray("glowItems")) {
                if (entry.isJsonPrimitive) {
                    legacy.items.add(entry.asString)
                }
            }
        }
        config.droppedItemGlow.filter.copyFrom(legacy)
        config.heldItemGlow.filter.copyFrom(legacy)
        config.heldGlint.filter.copyFrom(legacy)
    }
}
