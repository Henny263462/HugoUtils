package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt

abstract class Control(var bounds: UiRect = UiRect(0, 0, 0, 0)) {
    var enabled: Boolean = true
    var visible: Boolean = true
    open fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {}
    open fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = false
    open fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    open fun mouseReleased() {}
}

class Button(
    val label: String,
    val onClick: () -> Unit,
    bounds: UiRect = UiRect(0, 0, 0, 0)
) : Control(bounds) {
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        val t = UiDraw.theme
        UiDraw.panel(context, bounds, if (hovered) t.accentSoft else t.inset, if (hovered) t.accent else t.border)
        val shown = UiDraw.ellipsize(renderer, label, bounds.w - 8)
        context.drawText(renderer, shown, bounds.x + (bounds.w - renderer.getWidth(shown)) / 2, bounds.y + (bounds.h - 8) / 2, if (enabled) t.text else t.textDim, false)
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean =
        enabled && visible && bounds.contains(mouseX, mouseY).also { if (it) onClick() }
}

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
        val t = UiDraw.theme
        UiDraw.fill(context, bounds, Theme.lerpColor(t.border, t.accentMuted, animation.value))
        val knob = 4 + ((bounds.w - 12) * animation.value).roundToInt()
        UiDraw.fill(context, bounds.x + knob, bounds.y + 3, 8, bounds.h - 6, t.text)
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !visible || !bounds.contains(mouseX, mouseY)) return false
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
    fun set(value: Float, notify: Boolean = false) {
        this.value = value.coerceIn(min, max)
        if (notify) onChange(this.value)
    }
    private fun apply(mouseX: Double) {
        val progress = ((mouseX - bounds.x) / bounds.w.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        set(min + (max - min) * progress, true)
    }
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        val t = UiDraw.theme
        UiDraw.fill(context, bounds.x, bounds.y + bounds.h / 2 - 1, bounds.w, 3, t.border)
        val progress = if (max == min) 0f else (value - min) / (max - min)
        UiDraw.fill(context, bounds.x, bounds.y + bounds.h / 2 - 1, (bounds.w * progress).roundToInt(), 3, t.accent)
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !bounds.contains(mouseX, mouseY)) return false
        dragging = true
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
        val t = UiDraw.theme
        UiDraw.panel(context, bounds, t.inset, if (focused) t.accent else t.border)
        val shown = logic.text.ifEmpty { placeholder }
        context.drawText(renderer, UiDraw.ellipsize(renderer, shown, bounds.w - 8), bounds.x + 4, bounds.y + (bounds.h - 8) / 2, if (logic.text.isEmpty()) t.textDim else t.text, false)
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        focused = bounds.contains(mouseX, mouseY)
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
}

open class Card(bounds: UiRect = UiRect(0, 0, 0, 0), var title: String = "") : Control(bounds) {
    val children = mutableListOf<Control>()
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        UiDraw.panel(context, bounds, UiDraw.theme.card, UiDraw.theme.border)
        if (title.isNotEmpty()) context.drawText(renderer, title, bounds.x + 8, bounds.y + 8, UiDraw.theme.text, false)
        children.filter { it.visible }.forEach { it.render(context, renderer, mouseX, mouseY) }
    }
}

class Tabs<T>(var values: List<T>, var selected: T, val label: (T) -> String = { it.toString() }, val onSelect: (T) -> Unit = {}) {
    fun select(value: T): Boolean {
        if (value !in values) return false
        selected = value
        onSelect(value)
        return true
    }
}

class Dropdown<T>(items: List<T>, val label: (T) -> String = { it.toString() }, val onSelect: (T) -> Unit = {}) {
    var items: List<T> = items
    var open = false
    var selected: T? = null
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
    val maxScroll: Int get() = (contentHeight - viewport.h).coerceAtLeast(0)
    fun scrollBy(pixels: Int): Int {
        scroll = (scroll + pixels).coerceIn(0, maxScroll)
        return scroll
    }
    fun reset() { scroll = 0 }
}
