package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW

class ProfileConfigPage : ConfigPage {
    override val category = ConfigCategory.PROFILES
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var input = UiRect(0, 0, 0, 0)
    private var buttons = emptyList<Pair<Action, UiRect>>()
    private val profileHits = ArrayList<Pair<String, UiRect>>()
    private var name = ""
    private var focused = false
    private var selected: String? = null
    private var pendingDelete: String? = null

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 220)
        input = UiRect(x + 10, y + 34, width - 20, 18)
        val buttonWidth = ((width - 20 - 8) / 3).coerceAtLeast(70)
        buttons = Action.entries.mapIndexed { index, action ->
            val row = index / 3
            val col = index % 3
            action to UiRect(x + 10 + col * (buttonWidth + 4), y + 60 + row * 24, buttonWidth, 20)
        }
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        val font = client.textRenderer
        UiDraw.panel(context, frame, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, "Profile", frame.x + 10, frame.y + 12, HugoTheme.text, false)
        UiDraw.panel(context, input, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = name.ifBlank { "Profilname…" }
        context.drawText(font, shown, input.x + 5, input.y + 5, if (name.isBlank()) HugoTheme.textDim else HugoTheme.text, false)
        buttons.forEach { (action, rect) ->
            val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
            UiDraw.fill(context, rect, if (hovered) HugoTheme.accentSoft else HugoTheme.inset)
            UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (hovered) HugoTheme.accent else HugoTheme.cardBorder)
            val label = if (action == Action.DELETE && pendingDelete == selected && selected != null) "Bestätigen" else action.label
            context.drawText(font, UiDraw.ellipsize(font, label, rect.w - 8), rect.x + 4, rect.y + 6, HugoTheme.textMuted, false)
        }

        profileHits.clear()
        var y = frame.y + 112
        for (profile in ConfigManager.profileNames()) {
            val rect = UiRect(frame.x + 10, y, frame.w - 20, 20)
            profileHits += profile to rect
            val active = profile == ConfigManager.activeProfile
            val chosen = profile == selected
            UiDraw.fill(context, rect, if (chosen || active) HugoTheme.accentSoft else HugoTheme.inset)
            UiDraw.border(context, rect.x, rect.y, rect.w, rect.h,
                if (chosen) HugoTheme.accent else if (active) HugoTheme.success else HugoTheme.cardBorder)
            val suffix = if (active) "  (geladen)" else ""
            context.drawText(font, profile + suffix, rect.x + 6, rect.y + 6, HugoTheme.text, false)
            y += 22
            if (y + 20 > frame.bottom() - 8) break
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        focused = input.contains(mouseX, mouseY)
        profileHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let {
            selected = it.first
            name = it.first
            pendingDelete = null
            return true
        }
        buttons.firstOrNull { it.second.contains(mouseX, mouseY) }?.let {
            execute(it.first)
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (!focused) return false
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (name.isNotEmpty()) name = name.dropLast(1)
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                execute(Action.SAVE_AS)
                true
            }
            else -> false
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!focused || !input.isValidChar || name.length >= 40) return false
        name += input.asString()
        return true
    }

    override fun persist() = ConfigManager.requestSave()

    private fun execute(action: Action) {
        when (action) {
            Action.LOAD -> selected?.let(ConfigManager::loadProfile)
            Action.SAVE -> selected?.let(ConfigManager::saveProfile)
            Action.SAVE_AS -> if (ConfigManager.saveProfile(name)) selected = ConfigManager.activeProfile
            Action.RENAME -> selected?.let { old ->
                if (ConfigManager.renameProfile(old, name)) selected = name.trim().lowercase().replace(' ', '_')
            }
            Action.DELETE -> selected?.let {
                if (pendingDelete == it) {
                    ConfigManager.deleteProfile(it)
                    selected = null
                    pendingDelete = null
                } else pendingDelete = it
            }
        }
    }

    private enum class Action(val label: String) {
        LOAD("Laden"),
        SAVE("Überschreiben"),
        SAVE_AS("Speichern als"),
        RENAME("Umbenennen"),
        DELETE("Löschen")
    }
}
