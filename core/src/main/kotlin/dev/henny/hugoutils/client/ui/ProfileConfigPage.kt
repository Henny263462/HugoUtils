package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class ProfileConfigPage : ConfigPage {
    override val category = ConfigCategory.PROFILES
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var input = UiRect(0, 0, 0, 0)
    private var saveButton = UiRect(0, 0, 0, 0)
    private var listFrame = UiRect(0, 0, 0, 0)
    private var actionBar = emptyList<Pair<Action, UiRect>>()
    private val profileHits = ArrayList<Pair<String, UiRect>>()
    private var name = ""
    private var focused = false
    private var selected: String? = null
    private var pendingDelete: String? = null
    private var status: String? = null
    private var statusAccent = false
    private var scroll = 0
    private var maxScroll = 0
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var caretTicks = 0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val h = height.coerceAtLeast(MIN_H)
        frame = UiRect(x, y, width, h)

        val saveW = 88
        input = UiRect(x + 10, y + 52, width - 20 - saveW - 6, 22)
        saveButton = UiRect(input.right() + 6, y + 52, saveW, 22)

        val actionY = y + h - 34
        val gap = 4
        val actionW = ((width - 20 - gap * 3) / 4).coerceAtLeast(56)
        actionBar = listOf(Action.LOAD, Action.SAVE, Action.RENAME, Action.DELETE).mapIndexed { index, action ->
            action to UiRect(x + 10 + index * (actionW + gap), actionY, actionW, 22)
        }

        val listTop = y + 92
        val listBottom = actionY - 28
        listFrame = UiRect(x + 10, listTop, width - 20, (listBottom - listTop).coerceAtLeast(48))

        val names = ConfigManager.profileNames()
        val contentH = if (names.isEmpty()) ROW_H else names.size * ROW_H
        maxScroll = (contentH - listFrame.h).coerceAtLeast(0)
        scroll = scroll.coerceIn(0, maxScroll)
        return frame.h
    }

    override fun resetUi() {
        focused = false
        pendingDelete = null
        status = null
        scroll = 0
        if (selected == null) {
            selected = ConfigManager.activeProfile
            name = selected.orEmpty()
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        caretTicks++
        val font = client.textRenderer
        val names = ConfigManager.profileNames()
        val active = ConfigManager.activeProfile

        UiDraw.panel(context, frame, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, "Profile", frame.x + 10, frame.y + 10, HugoTheme.text, false)
        val activeLabel = if (active.isNullOrBlank()) "Kein Profil geladen" else "Aktiv: $active"
        context.drawText(
            font,
            UiDraw.ellipsize(font, activeLabel, frame.w - 20),
            frame.x + 10,
            frame.y + 24,
            if (active.isNullOrBlank()) HugoTheme.textDim else HugoTheme.success,
            false
        )

        context.drawText(font, "Name", frame.x + 10, frame.y + 40, HugoTheme.textMuted, false)
        drawInput(context, font)
        drawButton(context, saveButton, "Speichern", canSave(), primary = true)

        context.drawText(font, "Gespeichert", listFrame.x, listFrame.y - 12, HugoTheme.textMuted, false)
        val count = names.size.toString()
        context.drawText(
            font,
            count,
            listFrame.right() - font.getWidth(count),
            listFrame.y - 12,
            HugoTheme.textDim,
            false
        )

        UiDraw.panel(context, listFrame, HugoTheme.inset, HugoTheme.cardBorder)
        profileHits.clear()
        context.enableScissor(listFrame.x, listFrame.y, listFrame.right(), listFrame.bottom())
        if (names.isEmpty()) {
            context.drawText(
                font,
                "Noch keine Profile — Namen eingeben und speichern.",
                listFrame.x + 10,
                listFrame.y + listFrame.h / 2 - 4,
                HugoTheme.textDim,
                false
            )
        } else {
            var rowY = listFrame.y - scroll
            for (profile in names) {
                val row = UiRect(listFrame.x + 1, rowY, listFrame.w - 2, ROW_H)
                if (row.bottom() >= listFrame.y && row.y <= listFrame.bottom()) {
                    drawProfileRow(context, font, profile, row, active)
                }
                if (row.bottom() > listFrame.y && row.y < listFrame.bottom()) {
                    profileHits += profile to row
                }
                rowY += ROW_H
            }
        }
        context.disableScissor()
        drawListScrollbar(context)

        for ((action, rect) in actionBar) {
            val label = when {
                action == Action.DELETE && pendingDelete != null && pendingDelete == selected -> "Bestätigen"
                else -> action.label
            }
            drawButton(
                context,
                rect,
                label,
                actionEnabled(action),
                primary = false,
                danger = action == Action.DELETE && pendingDelete == selected
            )
        }

        status?.let { message ->
            context.drawText(
                font,
                UiDraw.ellipsize(font, message, frame.w - 24),
                frame.x + 12,
                actionBar.firstOrNull()?.second?.y?.minus(14) ?: (frame.bottom() - 48),
                if (statusAccent) HugoTheme.accent else HugoTheme.textDim,
                false
            )
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (input.contains(mouseX, mouseY)) {
            focused = true
            return true
        }
        focused = false

        if (saveButton.contains(mouseX, mouseY) && canSave()) {
            execute(Action.SAVE_AS)
            return true
        }

        profileHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (profile, _) ->
            selected = profile
            name = profile
            pendingDelete = null
            status = null
            return true
        }

        actionBar.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (action, _) ->
            if (actionEnabled(action)) {
                execute(action)
                return true
            }
            return true
        }

        if (listFrame.contains(mouseX, mouseY)) return true
        return frame.contains(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!listFrame.contains(mouseX, mouseY) || maxScroll <= 0) return false
        scroll = (scroll - (amount * 16).roundToInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (pendingDelete != null) {
                pendingDelete = null
                return true
            }
            if (focused) {
                focused = false
                return true
            }
        }
        if (!focused) return false
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (name.isNotEmpty()) name = name.dropLast(1)
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (canSave()) execute(Action.SAVE_AS)
                true
            }
            else -> false
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!focused || !input.isValidChar || name.length >= 40) return false
        name += input.asString()
        pendingDelete = null
        return true
    }

    override fun persist() = ConfigManager.requestSave()

    private fun drawInput(context: DrawContext, font: net.minecraft.client.font.TextRenderer) {
        val hovered = input.contains(lastMouseX, lastMouseY)
        UiDraw.panel(
            context,
            input,
            HugoTheme.inset,
            when {
                focused -> HugoTheme.accent
                hovered -> HugoTheme.accentMuted
                else -> HugoTheme.cardBorder
            }
        )
        val placeholder = name.isBlank()
        val shown = if (placeholder) "z. B. pvp, stream…" else name
        val textColor = if (placeholder) HugoTheme.textDim else HugoTheme.text
        context.drawText(font, UiDraw.ellipsize(font, shown, input.w - 12), input.x + 6, input.y + 7, textColor, false)
        if (focused && !placeholder && (caretTicks / 10) % 2 == 0) {
            val caretX = input.x + 6 + font.getWidth(UiDraw.ellipsize(font, name, input.w - 12))
            if (caretX < input.right() - 4) {
                UiDraw.fill(context, caretX, input.y + 5, 1, input.h - 10, HugoTheme.accent)
            }
        }
    }

    private fun drawProfileRow(
        context: DrawContext,
        font: net.minecraft.client.font.TextRenderer,
        profile: String,
        row: UiRect,
        active: String?
    ) {
        val chosen = profile == selected
        val isActive = profile == active
        val hovered = row.contains(lastMouseX, lastMouseY)
        when {
            chosen -> UiDraw.fill(context, row, HugoTheme.accentSoft)
            hovered -> UiDraw.fill(context, row, 0x14FFFFFF)
            isActive -> UiDraw.fill(context, row, 0x183DDC97)
        }
        if (chosen) {
            context.fill(row.x, row.y + 3, row.x + 2, row.bottom() - 3, HugoTheme.accent)
        } else if (isActive) {
            context.fill(row.x, row.y + 3, row.x + 2, row.bottom() - 3, HugoTheme.success)
        }

        val nameMax = if (isActive) row.w - 54 else row.w - 14
        context.drawText(
            font,
            UiDraw.ellipsize(font, profile, nameMax),
            row.x + 10,
            row.y + 8,
            HugoTheme.text,
            false
        )
        if (isActive) {
            val badge = "AKTIV"
            val bw = font.getWidth(badge) + 8
            val badgeRect = UiRect(row.right() - bw - 6, row.y + 5, bw, 12)
            UiDraw.fill(context, badgeRect, 0x333DDC97)
            UiDraw.border(context, badgeRect.x, badgeRect.y, badgeRect.w, badgeRect.h, HugoTheme.success)
            context.drawText(font, badge, badgeRect.x + 4, badgeRect.y + 2, HugoTheme.success, false)
        }
    }

    private fun drawListScrollbar(context: DrawContext) {
        if (maxScroll <= 0) return
        val trackX = listFrame.right() - 4
        val trackY = listFrame.y + 3
        val trackH = listFrame.h - 6
        UiDraw.fill(context, trackX, trackY, 2, trackH, HugoTheme.cardBorder)
        val thumbH = (trackH * listFrame.h / (listFrame.h + maxScroll).toFloat()).roundToInt().coerceIn(10, trackH)
        val thumbY = trackY + ((trackH - thumbH) * (scroll / maxScroll.toFloat())).roundToInt()
        UiDraw.fill(context, trackX, thumbY, 2, thumbH, HugoTheme.accentMuted)
    }

    private fun drawButton(
        context: DrawContext,
        rect: UiRect,
        label: String,
        enabled: Boolean,
        primary: Boolean,
        danger: Boolean = false
    ) {
        val hovered = enabled && rect.contains(lastMouseX, lastMouseY)
        val fill = when {
            !enabled -> HugoTheme.inset
            danger && hovered -> 0x55FF6B6B
            danger -> 0x33FF6B6B
            primary && hovered -> HugoTheme.accentSoft
            primary -> 0x226EE7FF
            hovered -> HugoTheme.accentSoft
            else -> HugoTheme.inset
        }
        val border = when {
            !enabled -> HugoTheme.cardBorder
            danger -> HugoTheme.danger
            hovered || primary -> HugoTheme.accent
            else -> HugoTheme.cardBorder
        }
        UiDraw.fill(context, rect, fill)
        UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, border)
        val font = client.textRenderer
        val color = when {
            !enabled -> HugoTheme.textDim
            danger -> HugoTheme.danger
            else -> HugoTheme.text
        }
        val shown = UiDraw.ellipsize(font, label, rect.w - 8)
        context.drawText(
            font,
            shown,
            rect.x + (rect.w - font.getWidth(shown)) / 2,
            rect.y + 7,
            color,
            false
        )
    }

    private fun canSave(): Boolean = name.trim().isNotEmpty()

    private fun actionEnabled(action: Action): Boolean = when (action) {
        Action.LOAD, Action.SAVE, Action.RENAME, Action.DELETE -> selected != null
        Action.SAVE_AS -> canSave()
    }

    private fun execute(action: Action) {
        when (action) {
            Action.LOAD -> selected?.let { profile ->
                if (ConfigManager.loadProfile(profile)) {
                    setStatus("Profil „$profile“ geladen", accent = true)
                    pendingDelete = null
                } else {
                    setStatus("Laden fehlgeschlagen", accent = false)
                }
            }
            Action.SAVE -> selected?.let { profile ->
                if (ConfigManager.saveProfile(profile)) {
                    setStatus("Profil „$profile“ überschrieben", accent = true)
                    pendingDelete = null
                } else {
                    setStatus("Speichern fehlgeschlagen", accent = false)
                }
            }
            Action.SAVE_AS -> {
                val trimmed = name.trim()
                if (ConfigManager.saveProfile(trimmed)) {
                    selected = ConfigManager.activeProfile
                    name = selected.orEmpty()
                    setStatus("Profil „$selected“ gespeichert", accent = true)
                    pendingDelete = null
                    focused = false
                } else {
                    setStatus("Ungültiger Name oder Speichern fehlgeschlagen", accent = false)
                }
            }
            Action.RENAME -> selected?.let { old ->
                if (name.trim().isEmpty()) {
                    setStatus("Neuen Namen oben eingeben", accent = false)
                    focused = true
                    return
                }
                if (ConfigManager.renameProfile(old, name)) {
                    selected = name.trim().lowercase().replace(' ', '_')
                    name = selected.orEmpty()
                    setStatus("Umbenannt zu „$selected“", accent = true)
                    pendingDelete = null
                } else {
                    setStatus("Umbenennen fehlgeschlagen", accent = false)
                }
            }
            Action.DELETE -> selected?.let { profile ->
                if (pendingDelete == profile) {
                    if (ConfigManager.deleteProfile(profile)) {
                        setStatus("Profil „$profile“ gelöscht", accent = false)
                        selected = ConfigManager.activeProfile
                        name = selected.orEmpty()
                        pendingDelete = null
                    } else {
                        setStatus("Löschen fehlgeschlagen", accent = false)
                    }
                } else {
                    pendingDelete = profile
                    setStatus("Nochmal „Bestätigen“ zum Löschen", accent = false)
                }
            }
        }
    }

    private fun setStatus(message: String, accent: Boolean) {
        status = message
        statusAccent = accent
    }

    private enum class Action(val label: String) {
        LOAD("Laden"),
        SAVE("Überschreiben"),
        SAVE_AS("Speichern"),
        RENAME("Umbenennen"),
        DELETE("Löschen")
    }

    companion object {
        private const val MIN_H = 240
        private const val ROW_H = 24
    }
}
