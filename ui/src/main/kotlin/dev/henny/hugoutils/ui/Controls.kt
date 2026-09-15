package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

abstract class Control(var bounds: UiRect = UiRect(0, 0, 0, 0)) {
    var enabled: Boolean = true
    var visible: Boolean = true
    var tooltip: String? = null
    protected val interaction = InteractionState()
    open fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {}
    open fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = false
    open fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    open fun mouseReleased() {}
}

enum class ButtonStyle { PRIMARY, SECONDARY, GHOST, DANGER }

open class Button(
    val label: String,
    val onClick: () -> Unit,
    bounds: UiRect = UiRect(0, 0, 0, 0),
    var style: ButtonStyle = ButtonStyle.SECONDARY
) : Control(bounds) {
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered)
        val t = UiDraw.theme
        val base = when (style) {
            ButtonStyle.PRIMARY -> t.accentMuted
            ButtonStyle.SECONDARY -> t.inset
            ButtonStyle.GHOST -> t.sidebar
            ButtonStyle.DANGER -> t.withAlpha(t.danger, 45)
        }
        val hover = when (style) {
            ButtonStyle.PRIMARY -> t.accent
            ButtonStyle.DANGER -> t.danger
            else -> t.surfaceRaised
        }
        val fill = Theme.lerpColor(base, hover, interaction.hover.value * .72f)
        val border = Theme.lerpColor(t.border, if (style == ButtonStyle.DANGER) t.danger else t.accent, interaction.hover.value)
        UiDraw.shadow(context, bounds, .25f * interaction.hover.value)
        UiDraw.panel(context, bounds, fill, border)
        val shown = UiDraw.ellipsize(renderer, label, bounds.w - 8)
        val offset = interaction.press.value.roundToInt()
        context.drawText(renderer, shown, bounds.x + (bounds.w - renderer.getWidth(shown)) / 2, bounds.y + (bounds.h - 8) / 2 + offset, if (enabled) t.text else t.textDim, false)
        if (hovered) tooltip?.let {
            val window = MinecraftClient.getInstance().window
            UiDraw.tooltip(context, renderer, it, mouseX, mouseY, window.scaledWidth, window.scaledHeight)
        }
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !visible || !bounds.contains(mouseX, mouseY)) return false
        interaction.pulse()
        onClick()
        return true
    }
}

class IconButton(
    icon: String,
    onClick: () -> Unit,
    bounds: UiRect = UiRect(0, 0, UiMetrics.CONTROL_HEIGHT, UiMetrics.CONTROL_HEIGHT),
    style: ButtonStyle = ButtonStyle.GHOST
) : Button(icon, onClick, bounds, style)

class Toggle(
    initialValue: Boolean = false,
    bounds: UiRect = UiRect(0, 0, 32, 14),
    val onChange: (Boolean) -> Unit = {}
) : Control(bounds) {
    var value = initialValue
        private set
    private val animation = Animation(if (initialValue) 1f else 0f, .14f)
    fun set(value: Boolean, notify: Boolean = false) {
        this.value = value
        animation.animateTo(if (value) 1f else 0f)
        if (notify) onChange(value)
    }
    fun tick(deltaSeconds: Float) = animation.update(deltaSeconds)
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered)
        animation.update(UiFrame.deltaSeconds)
        val t = UiDraw.theme
        val track = Theme.lerpColor(t.trackOff, t.accentMuted, animation.value)
        UiDraw.panel(context, bounds, Theme.lerpColor(track, t.surfaceRaised, interaction.hover.value * .25f), Theme.lerpColor(t.border, t.accent, interaction.hover.value))
        val knob = 4 + ((bounds.w - 12) * animation.value).roundToInt()
        val knobRect = UiRect(bounds.x + knob, bounds.y + 3, 8, bounds.h - 6)
        UiDraw.shadow(context, knobRect, .35f)
        UiDraw.fill(context, knobRect, t.knob)
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !visible || !bounds.contains(mouseX, mouseY)) return false
        interaction.pulse()
        set(!value, true)
        return true
    }
}

class Slider(
    initialValue: Float = 0f,
    var min: Float = 0f,
    var max: Float = 1f,
    val onChange: (Float) -> Unit = {},
    bounds: UiRect = UiRect(0, 0, 100, 12)
) : Control(bounds) {
    var value = initialValue.coerceIn(min, max)
        private set
    private var dragging = false
    private val displayed = AnimatedFloat(value, .1f)
    fun set(value: Float, notify: Boolean = false) {
        this.value = value.coerceIn(min, max)
        if (notify) onChange(this.value)
    }
    private fun apply(mouseX: Double) {
        val progress = ((mouseX - bounds.x) / bounds.w.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        set(min + (max - min) * progress, true)
    }
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered, dragging)
        val t = UiDraw.theme
        val progress = if (max == min) 0f else (value - min) / (max - min)
        displayed.animateTo(progress)
        displayed.update(UiFrame.deltaSeconds)
        val trackY = bounds.y + bounds.h / 2 - 1
        UiDraw.fill(context, bounds.x, trackY, bounds.w, 3, Theme.lerpColor(t.border, t.textDim, interaction.hover.value))
        val filled = (bounds.w * displayed.value).roundToInt()
        UiDraw.fill(context, bounds.x, trackY, filled, 3, t.accent)
        val knobX = (bounds.x + filled).coerceIn(bounds.x + 2, bounds.right - 2)
        val radius = if (hovered || dragging) 4 else 3
        UiDraw.fill(context, knobX - radius, bounds.y + bounds.h / 2 - radius, radius * 2, radius * 2, t.knob)
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !bounds.contains(mouseX, mouseY)) return false
        dragging = true
        interaction.pulse()
        apply(mouseX)
        return true
    }
    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        if (!dragging) return false
        apply(mouseX)
        return true
    }
    override fun mouseReleased() { dragging = false }
}

class TextFieldLogic(
    text: String = "",
    var maxLength: Int = Int.MAX_VALUE
) {
    var text: String = text.take(maxLength)
        private set
    var cursor: Int = this.text.length
        private set
    var anchor: Int = cursor
        private set
    val selection: IntRange? get() {
        if (cursor == anchor) return null
        return minOf(cursor, anchor) until maxOf(cursor, anchor)
    }
    val selectedText: String get() = selection?.let { text.substring(it.first, it.last + 1) }.orEmpty()

    fun setText(value: String) {
        text = value.take(maxLength)
        cursor = text.length
        anchor = cursor
    }
    fun selectAll() { anchor = 0; cursor = text.length }
    fun moveCursor(offset: Int, selecting: Boolean = false) {
        cursor = (cursor + offset).coerceIn(0, text.length)
        if (!selecting) anchor = cursor
    }
    fun insert(value: String) {
        deleteSelection()
        val insertion = value.take((maxLength - text.length).coerceAtLeast(0))
        text = text.substring(0, cursor) + insertion + text.substring(cursor)
        cursor += insertion.length
        anchor = cursor
    }
    fun backspace() {
        if (deleteSelection()) return
        if (cursor > 0) {
            text = text.removeRange(cursor - 1, cursor)
            cursor--
            anchor = cursor
        }
    }
    fun delete() {
        if (deleteSelection()) return
        if (cursor < text.length) text = text.removeRange(cursor, cursor + 1)
        anchor = cursor
    }
    fun copy(): String = selectedText
    fun cut(): String = copy().also { deleteSelection() }
    fun paste(value: String) = insert(value)
    private fun deleteSelection(): Boolean {
        val range = selection ?: return false
        text = text.removeRange(range.first, range.last + 1)
        cursor = range.first
        anchor = cursor
        return true
    }
}

class TextField(
    val logic: TextFieldLogic = TextFieldLogic(),
    bounds: UiRect = UiRect(0, 0, 100, 20),
    var placeholder: String = "",
    val onChange: (String) -> Unit = {}
) : Control(bounds) {
    var focused = false
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered, focused = focused)
        val t = UiDraw.theme
        val border = Theme.lerpColor(t.border, t.focusRing, maxOf(interaction.hover.value * .45f, interaction.focus.value))
        UiDraw.panel(context, bounds, Theme.lerpColor(t.inset, t.surfacePressed, interaction.focus.value * .5f), border)
        val shown = logic.text.ifEmpty { placeholder }
        logic.selection?.let { selection ->
            val startX = bounds.x + 4 + renderer.getWidth(logic.text.substring(0, selection.first))
            val endX = bounds.x + 4 + renderer.getWidth(logic.text.substring(0, selection.last + 1))
            UiDraw.fill(
                context,
                startX.coerceAtMost(bounds.right - 3),
                bounds.y + 3,
                (endX - startX).coerceAtLeast(1).coerceAtMost(bounds.right - startX - 3),
                bounds.h - 6,
                t.accentSoft
            )
        }
        context.drawText(renderer, UiDraw.ellipsize(renderer, shown, bounds.w - 8), bounds.x + 4, bounds.y + (bounds.h - 8) / 2, if (logic.text.isEmpty()) t.textDim else t.text, false)
        if (focused && logic.selection == null && (UiFrame.elapsedSeconds * 2f).toInt() % 2 == 0) {
            val beforeCursor = logic.text.take(logic.cursor)
            val cursorX = (bounds.x + 4 + renderer.getWidth(beforeCursor)).coerceAtMost(bounds.right - 3)
            UiDraw.fill(context, cursorX, bounds.y + 4, 1, bounds.h - 8, t.text)
        }
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        focused = bounds.contains(mouseX, mouseY)
        if (focused) interaction.pulse()
        return focused
    }
    fun type(value: String): Boolean {
        if (!focused) return false
        logic.insert(value)
        onChange(logic.text)
        return true
    }
    fun backspace(): Boolean {
        if (!focused) return false
        logic.backspace()
        onChange(logic.text)
        return true
    }
    fun copy(clipboard: (String) -> Unit): Boolean {
        if (!focused) return false
        clipboard(logic.copy())
        return true
    }
    fun paste(clipboard: () -> String): Boolean {
        if (!focused) return false
        logic.paste(clipboard())
        onChange(logic.text)
        return true
    }

    fun keyPressed(input: KeyInput, clipboard: () -> String, setClipboard: (String) -> Unit): Boolean {
        if (!focused) return false
        if (input.isPaste) return paste(clipboard)
        if (input.isCopy) return copy(setClipboard)
        val selecting = input.modifiers() and GLFW.GLFW_MOD_SHIFT != 0
        return when (input.key()) {
            GLFW.GLFW_KEY_A -> {
                if (input.modifiers() and GLFW.GLFW_MOD_CONTROL == 0) return false
                logic.selectAll()
                true
            }
            GLFW.GLFW_KEY_BACKSPACE -> backspace()
            GLFW.GLFW_KEY_DELETE -> {
                logic.delete()
                onChange(logic.text)
                true
            }
            GLFW.GLFW_KEY_LEFT -> {
                logic.moveCursor(-1, selecting)
                true
            }
            GLFW.GLFW_KEY_RIGHT -> {
                logic.moveCursor(1, selecting)
                true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                focused = false
                true
            }
            else -> false
        }
    }

    fun charTyped(input: CharInput): Boolean =
        if (focused && input.isValidChar) type(input.asString()) else false
}

open class Card(bounds: UiRect = UiRect(0, 0, 0, 0), var title: String = "") : Control(bounds) {
    val children = mutableListOf<Control>()
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered)
        UiDraw.shadow(context, bounds, .2f + interaction.hover.value * .18f)
        UiDraw.panel(context, bounds, Theme.lerpColor(UiDraw.theme.card, UiDraw.theme.cardHover, interaction.hover.value * .55f), Theme.lerpColor(UiDraw.theme.border, UiDraw.theme.accentMuted, interaction.hover.value * .35f))
        if (title.isNotEmpty()) context.drawText(renderer, title, bounds.x + UiMetrics.CARD_PADDING, bounds.y + UiMetrics.CARD_PADDING, UiDraw.theme.text, false)
        children.filter { it.visible }.forEach { it.render(context, renderer, mouseX, mouseY) }
    }
}

class Tabs<T>(
    var values: List<T>,
    var selected: T,
    val label: (T) -> String = { it.toString() },
    val onSelect: (T) -> Unit = {},
    bounds: UiRect = UiRect(0, 0, 160, UiMetrics.CONTROL_HEIGHT)
) : Control(bounds) {
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible || values.isEmpty()) return
        val width = bounds.w / values.size
        values.forEachIndexed { index, value ->
            val row = UiRect(bounds.x + index * width, bounds.y, if (index == values.lastIndex) bounds.right - (bounds.x + index * width) else width, bounds.h)
            val hovered = enabled && row.contains(mouseX.toDouble(), mouseY.toDouble())
            UiDraw.panel(
                context,
                row,
                if (value == selected) UiDraw.theme.accentSoft else if (hovered) UiDraw.theme.surfaceRaised else UiDraw.theme.inset,
                if (value == selected) UiDraw.theme.accent else UiDraw.theme.border
            )
            val text = UiDraw.ellipsize(renderer, label(value), row.w - 8)
            context.drawText(renderer, text, row.x + (row.w - renderer.getWidth(text)) / 2, row.y + (row.h - 8) / 2, if (value == selected) UiDraw.theme.text else UiDraw.theme.textMuted, false)
        }
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !visible || !bounds.contains(mouseX, mouseY) || values.isEmpty()) return false
        val index = (((mouseX - bounds.x) / bounds.w) * values.size).toInt().coerceIn(0, values.lastIndex)
        return select(values[index])
    }
    fun select(value: T): Boolean {
        if (value !in values) return false
        selected = value
        onSelect(value)
        return true
    }
}

class Dropdown<T>(
    items: List<T>,
    val label: (T) -> String = { it.toString() },
    val onSelect: (T) -> Unit = {},
    bounds: UiRect = UiRect(0, 0, 160, UiMetrics.CONTROL_HEIGHT)
) : Control(bounds) {
    var items: List<T> = items
    var open = false
    var selected: T? = null
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered, focused = open)
        UiDraw.panel(context, bounds, if (hovered || open) UiDraw.theme.surfaceRaised else UiDraw.theme.inset, if (open) UiDraw.theme.accent else UiDraw.theme.border)
        val shown = selected?.let(label) ?: "Auswählen …"
        context.drawText(renderer, UiDraw.ellipsize(renderer, shown, bounds.w - 22), bounds.x + 7, bounds.y + (bounds.h - 8) / 2, if (selected == null) UiDraw.theme.textMuted else UiDraw.theme.text, false)
        context.drawText(renderer, if (open) "▲" else "▼", bounds.right - 14, bounds.y + (bounds.h - 8) / 2, UiDraw.theme.textMuted, false)
        if (open) {
            items.take(8).forEachIndexed { index, value ->
                val row = UiRect(bounds.x, bounds.bottom + index * bounds.h, bounds.w, bounds.h)
                val rowHover = row.contains(mouseX.toDouble(), mouseY.toDouble())
                UiDraw.panel(context, row, if (rowHover) UiDraw.theme.surfaceRaised else UiDraw.theme.panel, UiDraw.theme.border)
                context.drawText(renderer, UiDraw.ellipsize(renderer, label(value), row.w - 12), row.x + 7, row.y + (row.h - 8) / 2, UiDraw.theme.text, false)
            }
        }
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !visible) return false
        if (bounds.contains(mouseX, mouseY)) {
            open = !open
            interaction.pulse()
            return true
        }
        if (open) {
            val index = ((mouseY - bounds.bottom) / bounds.h).toInt()
            if (index in 0 until minOf(items.size, 8) && mouseX >= bounds.x && mouseX < bounds.right) {
                return select(items[index])
            }
            open = false
        }
        return false
    }
    fun select(value: T): Boolean {
        if (value !in items) return false
        selected = value
        open = false
        onSelect(value)
        return true
    }
}

class SearchList<T>(items: List<T>, val searchText: (T) -> String = { it.toString() }) {
    var items: List<T> = items
    var query: String = ""
    val filtered: List<T> get() {
        val needle = query.trim().lowercase()
        return if (needle.isEmpty()) items else items.filter { needle in searchText(it).lowercase() }
    }
}

class ScrollPane(var viewport: UiRect = UiRect(0, 0, 0, 0)) {
    var contentHeight: Int = 0
    var scroll: Int = 0
        private set
    private val animatedScroll = AnimatedFloat(0f, .1f)
    val displayedScroll: Float
        get() {
            animatedScroll.animateTo(scroll.toFloat())
            return animatedScroll.update(UiFrame.deltaSeconds)
        }
    val maxScroll: Int get() = (contentHeight - viewport.h).coerceAtLeast(0)
    fun scrollBy(pixels: Int): Int {
        scroll = (scroll + pixels).coerceIn(0, maxScroll)
        return scroll
    }
    fun reset() { scroll = 0 }
}
