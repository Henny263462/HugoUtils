package dev.henny.hugoutils.client.gui.capture

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object GuiCaptureStore {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val root: Path by lazy {
        FabricLoader.getInstance().configDir
            .resolve("hugoutils")
            .resolve("gui-knowledge")
    }

    fun save(snapshot: GuiCaptureSnapshot, draft: GuiCaptureDraft): Path {
        val screenId = sanitizeScreenId(draft.screenId)
        require(screenId.isNotBlank()) { "screenId darf nicht leer sein" }
        require(draft.openType in setOf("command", "npc", "click")) {
            "openHow.type muss command, npc oder click sein"
        }
        require(draft.annotations.keys.all { it in snapshot.slots.indices }) {
            "Annotation verweist auf einen unbekannten Slot"
        }
        require(validItemSlots(draft.itemSlots, snapshot.slotCount)) {
            "itemSlots muss leer oder eine Liste aus Slots/Ranges sein"
        }
        val directory = root.resolve(screenId)
        Files.createDirectories(directory)
        writeAtomic(directory.resolve("capture.json"), captureJson(screenId, snapshot, draft))
        writeAtomic(directory.resolve("index.json"), indexJson(screenId, snapshot, draft))
        return directory
    }

    fun sanitizeScreenId(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-', '.', '_')
            .take(64)

    internal fun validItemSlots(value: String, slotCount: Int): Boolean {
        if (value.isBlank()) return true
        return value.split(',').all { token ->
            val parts = token.trim().split('-')
            when (parts.size) {
                1 -> parts[0].toIntOrNull()?.let { it in 0 until slotCount } == true
                2 -> {
                    val start = parts[0].toIntOrNull()
                    val end = parts[1].toIntOrNull()
                    start != null && end != null && start <= end &&
                        start in 0 until slotCount && end in 0 until slotCount
                }
                else -> false
            }
        }
    }

    private fun captureJson(
        screenId: String,
        snapshot: GuiCaptureSnapshot,
        draft: GuiCaptureDraft
    ) = JsonObject().apply {
        addProperty("schema", "hugobot.gui-capture.v1")
        addProperty("capturedAt", snapshot.capturedAt)
        add("openHow", openHow(draft))
        add("screen", JsonObject().apply {
            addProperty("id", screenId)
            addProperty("title", snapshot.title)
            add("titleRaw", snapshot.titleRaw)
            nullableProperty("type", snapshot.type)
            addProperty("handlerClass", snapshot.handlerClass)
            addProperty("clientScreenClass", snapshot.clientScreenClass)
            nullableProperty("rows", snapshot.rows)
            addProperty("slotCount", snapshot.slotCount)
            nullableProperty("inventoryStart", snapshot.inventoryStart)
            addProperty("syncId", snapshot.syncId)
            addProperty("backgroundWidth", snapshot.backgroundWidth)
            addProperty("backgroundHeight", snapshot.backgroundHeight)
        })
        add("slots", JsonArray().apply {
            snapshot.slots.forEach { frozen ->
                add(JsonObject().apply {
                    addProperty("slot", frozen.slot)
                    addProperty("slotId", frozen.slotId)
                    addProperty("inventoryIndex", frozen.inventoryIndex)
                    addProperty("x", frozen.x)
                    addProperty("y", frozen.y)
                    addProperty("playerInventory", frozen.playerInventory)
                    addProperty("hotbar", frozen.hotbar)
                    addProperty("empty", frozen.stack.isEmpty)
                    nullableProperty("itemId", frozen.itemId)
                    addProperty("count", frozen.stack.count)
                    addProperty("displayName", frozen.displayName)
                    add("lorePlain", JsonArray().also { array ->
                        frozen.lorePlain.forEach(array::add)
                    })
                    add("components", frozen.components ?: JsonObject())
                    add("nbt", frozen.nbt ?: JsonNull.INSTANCE)
                })
            }
        })
    }

    private fun indexJson(
        screenId: String,
        snapshot: GuiCaptureSnapshot,
        draft: GuiCaptureDraft
    ) = JsonObject().apply {
        addProperty("schema", "hugobot.gui-index.v1")
        addProperty("screenId", screenId)
        addProperty("titlePattern", draft.titlePattern)
        addProperty("purpose", draft.purpose)
        add("opensVia", openHow(draft))
        add("layout", JsonObject().apply {
            addProperty("description", draft.layout)
            nullableProperty("rows", snapshot.rows)
            addProperty("slotCount", snapshot.slotCount)
            nullableProperty("inventoryStart", snapshot.inventoryStart)
            addProperty("backgroundWidth", snapshot.backgroundWidth)
            addProperty("backgroundHeight", snapshot.backgroundHeight)
        })
        addProperty("notes", draft.notes)
        addProperty("itemSlots", draft.itemSlots)
        add("slots", JsonObject().apply {
            draft.annotations.toSortedMap().forEach { (slot, annotation) ->
                add(slot.toString(), JsonObject().apply {
                    addProperty("role", annotation.role.id)
                    addProperty("label", annotation.label)
                    addProperty("description", annotation.description)
                    add("clicks", JsonObject().apply {
                        addProperty("left", annotation.left)
                        addProperty("right", annotation.right)
                        addProperty("shift_left", annotation.shiftLeft)
                    })
                })
            }
        })
    }

    private fun openHow(draft: GuiCaptureDraft) = JsonObject().apply {
        addProperty("type", draft.openType)
        addProperty("detail", draft.openDetail)
    }

    private fun JsonObject.nullableProperty(name: String, value: String?) {
        if (value == null) add(name, JsonNull.INSTANCE) else addProperty(name, value)
    }

    private fun JsonObject.nullableProperty(name: String, value: Number?) {
        if (value == null) add(name, JsonNull.INSTANCE) else addProperty(name, value)
    }

    private fun writeAtomic(path: Path, value: JsonObject) {
        val temp = path.resolveSibling("${path.fileName}.tmp")
        Files.writeString(temp, gson.toJson(value) + "\n", StandardCharsets.UTF_8)
        try {
            Files.move(
                temp,
                path,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
