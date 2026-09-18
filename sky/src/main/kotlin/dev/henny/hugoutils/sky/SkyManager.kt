package dev.henny.hugoutils.sky

import com.google.gson.JsonObject
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.ConfigSection
import dev.henny.hugoutils.sky.mixin.CloudRendererInvoker
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import net.minecraft.util.profiler.Profilers
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object SkyManager : ConfigSection {
    override val id = "sky"
    @Volatile
    var skies: List<SkyPack> = emptyList()
        private set
    @Volatile
    var scanning = false
        private set
    @Volatile
    var applying = false
        private set
    @Volatile
    var status = "Sky-Texturepacks werden gesucht …"
        private set
    @Volatile
    private var selectedKey: String? = null
    private val busy = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "HugoUtils-Sky").apply { isDaemon = true }
    }
    private val activeTextures = LinkedHashMap<Identifier, NativeImageBackedTexture>()

    override fun read(json: JsonObject) {
        selectedKey = json.get("selectedPack")?.asString?.takeIf { it.isNotBlank() }
    }

    override fun write(): JsonObject = JsonObject().apply {
        selectedKey?.let { addProperty("selectedPack", it) }
    }

    fun selectedIndex(): Int = skies.indexOfFirst { it.key == selectedKey }.let { if (it < 0) 0 else it + 1 }

    fun scan(onDone: (() -> Unit)? = null) {
        if (!busy.compareAndSet(false, true)) return
        scanning = true
        status = "Durchsuche Resourcepacks asynchron …"
        val folder = MinecraftClient.getInstance().runDirectory.toPath().resolve("resourcepacks")
        executor.execute {
            val result = runCatching { SkyPackScanner.scan(folder) }
            MinecraftClient.getInstance().execute {
                skies = result.getOrDefault(emptyList())
                scanning = false
                busy.set(false)
                status = if (result.isFailure) {
                    "Resourcepacks konnten nicht gelesen werden."
                } else {
                    "${skies.size} Sky-Texturepacks gefunden."
                }
                onDone?.invoke()
                val selected = skies.indexOfFirst { it.key == selectedKey }
                if (selected >= 0 && activeTextures.isEmpty()) select(selected + 1)
            }
        }
    }

    fun select(index: Int, feedback: (String, Boolean) -> Unit = { _, _ -> }) {
        val pack = skies.getOrNull(index - 1)
        if (pack == null) {
            feedback("Sky $index existiert nicht. Verfügbar: 1–${skies.size}.", true)
            return
        }
        if (!busy.compareAndSet(false, true)) {
            feedback("Der Sky-Scanner arbeitet noch.", true)
            return
        }
        applying = true
        status = "Extrahiere Sky $index: ${pack.name} …"
        executor.execute {
            val loaded = runCatching {
                val folder = MinecraftClient.getInstance().runDirectory.toPath().resolve("resourcepacks")
                val extracted = SkyExtractor.extract(pack, folder, MAX_TEXTURE_BYTES)
                val images = linkedMapOf<String, ByteArray>()
                extracted.files.forEach { path ->
                    SkyPackScanner.readPath(extracted, path, MAX_TEXTURE_BYTES)?.let { images[path] = it }
                }
                ExtractedSky(pack, extracted, images)
            }
            MinecraftClient.getInstance().execute {
                val client = MinecraftClient.getInstance()
                val extracted = loaded.getOrElse {
                    applying = false
                    busy.set(false)
                    status = "Sky konnte nicht extrahiert werden."
                    feedback(status, true)
                    return@execute
                }
                selectedKey = pack.key
                ConfigManager.requestSave()
                status = "Wende nur den Sky-Extrakt von ${pack.name} an …"
                try {
                    enableExtractedPack(client, extracted.source, extracted.pack)
                    client.reloadResources().whenComplete { _, error ->
                        client.execute {
                            try {
                                if (error != null) {
                                    status = "Texturepack-Reload fehlgeschlagen, wende Sky direkt an."
                                }
                                install(extracted.images)
                                status = "Sky $index aktiv: ${pack.name} (nur Himmel)"
                                feedback(status, false)
                            } catch (_: Exception) {
                                status = "Sky konnte nicht geladen werden."
                                feedback(status, true)
                            } finally {
                                applying = false
                                busy.set(false)
                            }
                        }
                    }
                } catch (_: Exception) {
                    try {
                        install(extracted.images)
                        status = "Sky $index aktiv: ${pack.name} (nur Himmel)"
                        feedback(status, false)
                    } catch (_: Exception) {
                        status = "Sky konnte nicht geladen werden."
                        feedback(status, true)
                    } finally {
                        applying = false
                        busy.set(false)
                    }
                }
            }
        }
    }

    fun describe(): String =
        if (scanning) "Sky-Texturepacks werden noch gesucht …"
        else if (skies.isEmpty()) "Keine Sky-Änderungen in resourcepacks gefunden."
        else skies.mapIndexed { index, sky ->
            "${index + 1}: ${sky.name} (${sky.textures.joinToString { it.label }})"
        }.joinToString("\n")

    private fun enableExtractedPack(client: MinecraftClient, source: SkyPack, extracted: SkyPack) {
        val manager = client.resourcePackManager
        manager.scanPacks()
        val extractId = SkyExtractor.packProfileId(extracted.path.fileName.toString())
        val sourceId = SkyExtractor.sourceProfileId(source)
        require(manager.hasProfile(extractId)) { "Sky-Extrakt wurde nicht als Resourcepack erkannt." }
        val enabled = manager.enabledIds.toMutableList()
        enabled.removeAll { it.startsWith("file/${SkyExtractor.FOLDER_PREFIX}") }
        enabled.remove(sourceId)
        enabled.remove(extractId)
        enabled.add(extractId)
        manager.setEnabledProfiles(enabled)
        val next = client.options.resourcePacks
            .filterNot { it.startsWith("file/${SkyExtractor.FOLDER_PREFIX}") || it == sourceId }
            .toMutableList()
        if (extractId !in next) next.add(extractId)
        client.options.resourcePacks = next
        client.options.write()
    }

    private fun install(images: Map<String, ByteArray>) {
        val client = MinecraftClient.getInstance()
        val replacements = LinkedHashMap<Identifier, NativeImageBackedTexture>()
        try {
            val endPath = SkyTexture.END.resourcePath
            images[endPath]?.let { bytes ->
                val image = NativeImage.read(ByteArrayInputStream(bytes))
                val texture = NativeImageBackedTexture({ "HugoUtils End-Sky" }, image)
                replacements[textureId(endPath)] = texture
            }
            val previous = LinkedHashMap(activeTextures)
            activeTextures.clear()
            replacements.forEach { (id, texture) ->
                client.textureManager.registerTexture(id, texture)
                activeTextures[id] = texture
            }
            previous.forEach { (id, texture) ->
                if (id !in replacements) {
                    runCatching { client.textureManager.destroyTexture(id) }
                    runCatching { texture.close() }
                }
            }
            CelestialAtlasPatcher.apply(images)
            SkyTextureOverrides.setClouds(images[SkyTexture.CLOUDS.resourcePath])
            val cloudRenderer = client.worldRenderer.cloudRenderer
            val profiler = Profilers.get()
            val cloudAccess = cloudRenderer as CloudRendererInvoker
            val cloudCells = cloudAccess.`hugoutils$prepare`(client.resourceManager, profiler)
            cloudAccess.`hugoutils$apply`(cloudCells, client.resourceManager, profiler)
            client.worldRenderer.reload(client.resourceManager)
        } catch (error: Exception) {
            replacements.values.filter { it !in activeTextures.values }.forEach { runCatching { it.close() } }
            throw error
        }
    }

    private fun textureId(path: String): Identifier =
        Identifier.of("minecraft", path.removePrefix("assets/minecraft/"))

    private data class ExtractedSky(
        val source: SkyPack,
        val pack: SkyPack,
        val images: Map<String, ByteArray>
    )

    private const val MAX_TEXTURE_BYTES = 32 * 1024 * 1024
}
