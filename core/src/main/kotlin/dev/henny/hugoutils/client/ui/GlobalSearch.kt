package dev.henny.hugoutils.client.ui

import com.google.gson.JsonObject
import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.JsonView
import dev.henny.hugoutils.ui.TextFieldLogic
import dev.henny.hugoutils.ui.UiDraw
import dev.henny.hugoutils.ui.UiRect
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW
import java.util.concurrent.atomic.AtomicInteger

class GlobalSearch(
    private val onOpenPage: (String) -> Unit,
    private val onOpenItem: (String) -> Unit
) {
    private val query = TextFieldLogic(maxLength = 48)
    private val generation = AtomicInteger()
    var focused = false
        private set
    private var box = UiRect(0, 0, 0, 0)
    private var dropdown = UiRect(0, 0, 0, 0)
    private var hits = emptyList<Hit>()
    private var rowHits = emptyList<Pair<Hit, UiRect>>()
    private var caret = 0
    private var lastQuery = ""

    fun layout(x: Int, y: Int, w: Int) {
        box = UiRect(x, y, w, 20)
        dropdown = UiRect(x, y + 22, w, (hits.size * 18 + 8).coerceAtMost(160))
    }

    fun height(): Int = 26

    fun render(context: DrawContext, font: TextRenderer, mouseX: Int, mouseY: Int) {
        caret++
        UiDraw.panel(context, box, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = if (query.text.isEmpty()) "Suchen: Mods, Items, Settings …" else query.text
        context.drawText(
            font,
            UiDraw.ellipsize(font, shown, box.w - 12),
            box.x + 6,
            box.y + 6,
            if (query.text.isEmpty()) HugoTheme.textDim else HugoTheme.text,
            false
        )
        if (focused && (caret / 10) % 2 == 0) {
            val caretX = box.x + 6 + font.getWidth(query.text.take(40))
            UiDraw.fill(context, caretX, box.y + 4, 1, box.h - 8, HugoTheme.accent)
        }
        if (!focused || hits.isEmpty()) {
            rowHits = emptyList()
            return
        }
        dropdown = UiRect(box.x, box.bottom + 2, box.w, hits.size * 18 + 8)
        UiDraw.shadow(context, dropdown, .2f)
        UiDraw.panel(context, dropdown, HugoTheme.card, HugoTheme.cardBorder)
        rowHits = hits.take(8).mapIndexed { index, hit ->
            val row = UiRect(dropdown.x + 4, dropdown.y + 4 + index * 18, dropdown.w - 8, 16)
            val hovered = row.contains(mouseX.toDouble(), mouseY.toDouble())
            if (hovered) UiDraw.fill(context, row.x, row.y, row.w, row.h, HugoTheme.accentSoft)
            context.drawText(font, hit.kind, row.x + 4, row.y + 4, HugoTheme.textDim, false)
            context.drawText(
                font,
                UiDraw.ellipsize(font, hit.title, row.w - font.getWidth(hit.kind) - 16),
                row.x + font.getWidth(hit.kind) + 12,
                row.y + 4,
                HugoTheme.text,
                false
            )
            hit to row
        }
    }

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        rowHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let {
            activate(it.first)
            return true
        }
        if (box.contains(mouseX, mouseY)) {
            focused = true
            return true
        }
        focused = false
        return false
    }

    fun keyPressed(input: KeyInput): Boolean {
        if (!focused) return false
        if (input.isPaste) {
            query.paste(MinecraftClient.getInstance().keyboard.clipboard)
            refresh()
            return true
        }
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                query.backspace()
                refresh()
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                hits.firstOrNull()?.let(::activate)
                true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                focused = false
                true
            }
            else -> false
        }
    }

    fun charTyped(input: CharInput): Boolean {
        if (!focused || !input.isValidChar) return false
        query.insert(input.asString())
        refresh()
        return true
    }

    fun unfocus() {
        focused = false
    }

    fun contains(mouseX: Double, mouseY: Double): Boolean =
        box.contains(mouseX, mouseY) || (focused && hits.isNotEmpty() && dropdown.contains(mouseX, mouseY))

    private fun activate(hit: Hit) {
        focused = false
        when (hit.type) {
            "item", "listing" -> onOpenItem(hit.openId)
            "player" -> onOpenPage(ConfigCategory.MARKET_PLAYERS.id)
            else -> onOpenPage(hit.id)
        }
    }

    private fun refresh() {
        val text = query.text.trim()
        if (text == lastQuery) return
        lastQuery = text
        hits = localHits(text)
        if (text.length < 2 || !ClientSessionStore.hasToken()) return
        val request = generation.incrementAndGet()
        var remote = emptyList<Hit>()
        ClientJobs.submit({ _, error ->
            if (request != generation.get() || error) return@submit
            hits = (localHits(query.text.trim()) + remote).distinctBy { it.type to it.id }
        }) {
            val body = ClientApi.search(text, true)
            remote = JsonView.objects(body).mapNotNull(::remoteHit)
            "${remote.size} Treffer"
        }
    }

    private fun localHits(text: String): List<Hit> {
        if (text.isBlank()) return emptyList()
        return ConfigCategory.entries.filter { it.available && it.group != NavGroup.HIDDEN }
            .filter { it.title.contains(text, ignoreCase = true) || it.id.contains(text, ignoreCase = true) }
            .take(6)
            .map { category ->
                val kind = when (category.group) {
                    NavGroup.MODS -> "Mod"
                    NavGroup.SETTINGS -> "Setting"
                    NavGroup.MARKET -> "Market"
                    else -> "Seite"
                }
                Hit("page", category.id, category.title, kind, category.id)
            }
    }

    private fun remoteHit(obj: JsonObject): Hit? {
        val type = JsonView.str(obj, "type") ?: return null
        val id = JsonView.str(obj, "id") ?: return null
        val title = JsonView.str(obj, "title", "displayName", "name") ?: id
        val kind = when (type) {
            "item" -> "Item"
            "player" -> "Spieler"
            "listing" -> "Listing"
            else -> type
        }
        val openId = when (type) {
            "listing" -> JsonView.str(obj, "itemId", "minecraftId") ?: id
            else -> id
        }
        return Hit(type, id, title, kind, openId)
    }

    private data class Hit(
        val type: String,
        val id: String,
        val title: String,
        val kind: String,
        val openId: String
    )
}
