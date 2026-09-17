package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.Dialog
import dev.henny.hugoutils.ui.TextFieldLogic
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class ProfileConfigPage : ConfigPage {
    override val category = ConfigCategory.PROFILES
    override val id = "page:${category.id}"
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var hero = UiRect(0, 0, 0, 0)
    private var grid = UiRect(0, 0, 0, 0)
    private var createHit = UiRect(0, 0, 0, 0)
    private var cards = emptyList<Card>()
    private var menu: ContextMenu? = null
    private var status: String? = null
    private var statusAccent = false
    private var scroll = 0
    private var maxScroll = 0
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val names = ConfigManager.profileNames()
        val cols = columns(width)
        val rows = ((names.size + 1 + cols - 1) / cols).coerceAtLeast(1)
        val contentH = 86 + 18 + rows * (CARD_H + GAP)
        val h = contentH.coerceAtLeast(height.coerceAtLeast(260))
        frame = UiRect(x, y, width, h)
        hero = UiRect(x + 8, y + 8, width - 16, 58)
        val gridTop = hero.bottom() + 10
        grid = UiRect(x + 8, gridTop, width - 16, h - (gridTop - y) - 18)
        val cardW = ((grid.w - GAP * (cols - 1)) / cols).coerceAtLeast(96)
        val startY = grid.y - scroll
        val built = ArrayList<Card>(names.size)
        names.forEachIndexed { index, name ->
            val col = index % cols
            val row = index / cols
            built += Card(
                name,
                UiRect(grid.x + col * (cardW + GAP), startY + row * (CARD_H + GAP), cardW, CARD_H)
            )
        }
        val createIndex = names.size
        createHit = UiRect(
            grid.x + (createIndex % cols) * (cardW + GAP),
            startY + (createIndex / cols) * (CARD_H + GAP),
            cardW,
            CARD_H
        )
        cards = built
        maxScroll = (contentH - height).coerceAtLeast(0)
        scroll = scroll.coerceIn(0, maxScroll)
        return frame.h
    }

    override fun resetUi() {
        menu = null
        status = null
        scroll = 0
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = client.textRenderer
        val active = ConfigManager.activeProfile
        val count = ConfigManager.profileNames().size
        UiWidgets.hoverCard(context, hero, lastMouseX, lastMouseY, key = "profile-hero")
        context.drawText(font, "Profile", hero.x + 12, hero.y + 12, HugoTheme.text, false)
        val activeLabel = if (active.isNullOrBlank()) "Kein Profil geladen" else "Aktiv  $active"
        context.drawText(
            font,
            UiDraw.ellipsize(font, activeLabel, hero.w - 24),
            hero.x + 12,
            hero.y + 30,
            if (active.isNullOrBlank()) HugoTheme.textDim else HugoTheme.success,
            false
        )
        val countLabel = "$count gespeichert"
        context.drawText(font, countLabel, hero.right() - font.getWidth(countLabel) - 12, hero.y + 20, HugoTheme.textMuted, false)

        context.enableScissor(grid.x, grid.y, grid.right(), grid.bottom())
        cards.forEach { card ->
            if (card.rect.bottom() < grid.y || card.rect.y > grid.bottom()) return@forEach
            drawProfileCard(context, card, active)
        }
        if (createHit.bottom() >= grid.y && createHit.y <= grid.bottom()) {
            drawCreateCard(context)
        }
        context.disableScissor()
        status?.let { message ->
            context.drawText(
                font,
                UiDraw.ellipsize(font, message, frame.w - 24),
                frame.x + 10,
                frame.bottom() - 14,
                if (statusAccent) HugoTheme.accent else HugoTheme.textDim,
                false
            )
        }
        menu?.let { drawMenu(context, it) }
        UiDraw.scrollbar(context, grid, scroll, maxScroll)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean =
        mouseClicked(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_LEFT)

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val currentMenu = menu
        if (currentMenu != null) {
            currentMenu.items.firstOrNull { it.rect.contains(mouseX, mouseY) }?.let { item ->
                menu = null
                item.action()
                return true
            }
            menu = null
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) return frame.contains(mouseX, mouseY)
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            cards.firstOrNull { it.rect.contains(mouseX, mouseY) }?.let { card ->
                openMenu(card.name, mouseX.toInt(), mouseY.toInt())
                return true
            }
            return frame.contains(mouseX, mouseY)
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false
        if (createHit.contains(mouseX, mouseY)) {
            openCreate()
            return true
        }
        cards.firstOrNull { it.rect.contains(mouseX, mouseY) }?.let { card ->
            load(card.name)
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!grid.contains(mouseX, mouseY) || maxScroll <= 0) return false
        menu = null
        scroll = (scroll - (amount * 18).roundToInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && menu != null) {
            menu = null
            return true
        }
        return false
    }

    override fun persist() = ConfigManager.requestSave()

    private fun load(name: String) {
        if (ConfigManager.loadProfile(name)) {
            setStatus("Profil „$name“ geladen", true)
        } else {
            setStatus("Laden fehlgeschlagen", false)
        }
    }

    private fun openCreate() {
        PopupManager.open(
            ProfileNamePopup(
                title = "Neues Profil",
                initial = "",
                confirmLabel = "Erstellen"
            ) { name ->
                if (ConfigManager.saveProfile(name)) {
                    setStatus("Profil „$name“ erstellt", true)
                } else {
                    setStatus("Ungültiger Name oder Speichern fehlgeschlagen", false)
                }
            }
        )
    }

    private fun openMenu(profile: String, x: Int, y: Int) {
        val width = 118
        val height = MENU_ROW * 2 + 8
        val left = x.coerceIn(frame.x, (frame.right() - width).coerceAtLeast(frame.x))
        val top = y.coerceIn(frame.y, (frame.bottom() - height).coerceAtLeast(frame.y))
        val rename = UiRect(left + 4, top + 4, width - 8, MENU_ROW)
        val delete = UiRect(left + 4, rename.bottom(), width - 8, MENU_ROW)
        menu = ContextMenu(
            profile,
            UiRect(left, top, width, height),
            listOf(
                MenuItem("Umbenennen", rename) { openRename(profile) },
                MenuItem("Löschen", delete) { confirmDelete(profile) }
            )
        )
    }

    private fun openRename(profile: String) {
        PopupManager.open(
            ProfileNamePopup(
                title = "Profil umbenennen",
                initial = profile,
                confirmLabel = "Umbenennen"
            ) { name ->
                if (ConfigManager.renameProfile(profile, name)) {
                    setStatus("Umbenannt zu „$name“", true)
                } else {
                    setStatus("Umbenennen fehlgeschlagen", false)
                }
            }
        )
    }

    private fun confirmDelete(profile: String) {
        PopupManager.open(
            Dialog(
                title = "Profil löschen",
                message = "„$profile“ wirklich löschen?",
                confirmLabel = "Löschen",
                cancelLabel = "Abbrechen",
                onConfirm = {
                    if (ConfigManager.deleteProfile(profile)) {
                        setStatus("Profil „$profile“ gelöscht", false)
                    } else {
                        setStatus("Löschen fehlgeschlagen", false)
                    }
                }
            )
        )
    }

    private fun drawProfileCard(context: DrawContext, card: Card, active: String?) {
        val font = client.textRenderer
        val hovered = card.rect.contains(lastMouseX, lastMouseY)
        val isActive = card.name == active
        UiWidgets.hoverCard(context, card.rect, lastMouseX, lastMouseY, key = "profile-${card.name}", selected = isActive)
        val initial = card.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val badge = UiRect(card.rect.x + 10, card.rect.y + 14, 22, 22)
        UiDraw.panel(
            context,
            badge,
            if (isActive) HugoTheme.accentSoft else HugoTheme.inset,
            if (isActive) HugoTheme.success else HugoTheme.cardBorder
        )
        context.drawText(
            font,
            initial,
            badge.x + (badge.w - font.getWidth(initial)) / 2,
            badge.y + 7,
            if (isActive) HugoTheme.success else HugoTheme.text,
            false
        )
        context.drawText(
            font,
            UiDraw.ellipsize(font, card.name, card.rect.w - 48),
            card.rect.x + 40,
            card.rect.y + 14,
            HugoTheme.text,
            false
        )
        context.drawText(
            font,
            if (isActive) "Aktiv · Linksklick lädt" else "Linksklick lädt",
            card.rect.x + 40,
            card.rect.y + 30,
            if (isActive) HugoTheme.success else HugoTheme.textMuted,
            false
        )
        if (hovered) {
            context.drawText(font, "Rechtsklick: Umbenennen / Löschen", card.rect.x + 10, card.rect.bottom() - 14, HugoTheme.textDim, false)
        }
    }

    private fun drawCreateCard(context: DrawContext) {
        val font = client.textRenderer
        val hovered = createHit.contains(lastMouseX, lastMouseY)
        UiWidgets.hoverCard(context, createHit, lastMouseX, lastMouseY, key = "profile-create")
        val plus = "+"
        context.drawText(
            font,
            plus,
            createHit.x + (createHit.w - font.getWidth(plus)) / 2,
            createHit.y + 18,
            if (hovered) HugoTheme.accent else HugoTheme.text,
            false
        )
        val label = "Neues Profil"
        context.drawText(
            font,
            label,
            createHit.x + (createHit.w - font.getWidth(label)) / 2,
            createHit.y + 38,
            HugoTheme.textMuted,
            false
        )
    }

    private fun drawMenu(context: DrawContext, current: ContextMenu) {
        val font = client.textRenderer
        UiDraw.shadow(context, current.frame)
        UiDraw.panel(context, current.frame, HugoTheme.panel, HugoTheme.cardBorder)
        current.items.forEach { item ->
            val hovered = item.rect.contains(lastMouseX, lastMouseY)
            if (hovered) UiDraw.fill(context, item.rect, HugoTheme.accentSoft)
            val color = if (item.label == "Löschen") HugoTheme.danger else HugoTheme.text
            context.drawText(font, item.label, item.rect.x + 8, item.rect.y + 5, color, false)
        }
    }

    private fun setStatus(message: String, accent: Boolean) {
        status = message
        statusAccent = accent
    }

    private fun columns(width: Int): Int = when {
        width >= 520 -> 4
        width >= 360 -> 3
        else -> 2
    }

    private data class Card(val name: String, val rect: UiRect)
    private data class MenuItem(val label: String, val rect: UiRect, val action: () -> Unit)
    private data class ContextMenu(val profile: String, val frame: UiRect, val items: List<MenuItem>)

    companion object {
        private const val CARD_H = 72
        private const val GAP = 8
        private const val MENU_ROW = 18
    }
}

class ProfileNamePopup(
    private val title: String,
    initial: String,
    private val confirmLabel: String,
    private val onConfirm: (String) -> Unit
) : ConfigPopup {
    private val client = MinecraftClient.getInstance()
    private val field = TextFieldLogic(maxLength = 40).apply { setText(initial) }
    private var frame = UiRect(0, 0, 0, 0)
    private var input = UiRect(0, 0, 0, 0)
    private var confirm = UiRect(0, 0, 0, 0)
    private var cancel = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private var caret = 0
    private var focused = true

    override fun layout(screenWidth: Int, screenHeight: Int) {
        frame = UiRect((screenWidth - 280) / 2, (screenHeight - 118) / 2, 280, 118)
        input = UiRect(frame.x + 16, frame.y + 48, frame.w - 32, 22)
        cancel = UiRect(frame.right() - 168, frame.bottom() - 36, 70, 20)
        confirm = UiRect(frame.right() - 90, frame.bottom() - 36, 74, 20)
        close = UiRect(frame.right() - 24, frame.y + 10, 14, 14)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        caret++
        val font = client.textRenderer
        UiDraw.fill(context, 0, 0, client.window.scaledWidth, client.window.scaledHeight, 0xB0000000.toInt())
        UiDraw.shadow(context, frame)
        UiDraw.panel(context, frame, HugoTheme.panel, HugoTheme.panelBorder)
        context.drawText(font, title, frame.x + 16, frame.y + 14, HugoTheme.text, false)
        context.drawText(font, "×", close.x + 3, close.y + 2, HugoTheme.textMuted, false)
        UiDraw.panel(context, input, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = if (field.text.isEmpty()) "Name, z. B. pvp" else field.text
        context.drawText(
            font,
            UiDraw.ellipsize(font, shown, input.w - 12),
            input.x + 6,
            input.y + 7,
            if (field.text.isEmpty()) HugoTheme.textDim else HugoTheme.text,
            false
        )
        if (focused && field.text.isNotEmpty() && (caret / 10) % 2 == 0) {
            UiDraw.fill(context, input.x + 6 + font.getWidth(field.text), input.y + 5, 1, input.h - 10, HugoTheme.accent)
        }
        UiWidgets.button(context, font, cancel, "Abbrechen", mouseX.toDouble(), mouseY.toDouble(), true, ButtonStyle.GHOST)
        UiWidgets.button(
            context, font, confirm, confirmLabel, mouseX.toDouble(), mouseY.toDouble(),
            field.text.trim().isNotEmpty(), ButtonStyle.PRIMARY
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (close.contains(mouseX, mouseY) || !frame.contains(mouseX, mouseY) || cancel.contains(mouseX, mouseY)) {
            PopupManager.close()
            return true
        }
        if (input.contains(mouseX, mouseY)) {
            focused = true
            return true
        }
        focused = false
        if (confirm.contains(mouseX, mouseY)) {
            submit()
            return true
        }
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            PopupManager.close()
            return true
        }
        if (!focused) return true
        if (input.isPaste) {
            field.setText(client.keyboard.clipboard.take(40))
            return true
        }
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                field.backspace()
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                submit()
                true
            }
            else -> false
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!focused || !input.isValidChar) return true
        field.insert(input.asString())
        return true
    }

    private fun submit() {
        val name = field.text.trim()
        if (name.isEmpty()) return
        onConfirm(name)
        PopupManager.close()
    }
}