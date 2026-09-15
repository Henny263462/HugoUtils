package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ItemFilter
import dev.henny.hugoutils.client.config.ItemGlowFilter
import dev.henny.hugoutils.ui.TextFieldLogic
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import org.lwjgl.glfw.GLFW
import kotlin.math.ceil
import kotlin.math.roundToInt

class ItemFilterPanel(
    private val textRenderer: TextRenderer,
    private val filter: ItemFilter,
    private val onChange: () -> Unit
) {
    var card = UiRect(0, 0, 0, 0)
        private set

    private var modeHits = emptyList<Pair<ItemFilter.Mode, UiRect>>()
    private var searchBox = UiRect(0, 0, 0, 0)
    private var handButton = UiRect(0, 0, 0, 0)
    private var selectedArea = UiRect(0, 0, 0, 0)
    private var gridArea = UiRect(0, 0, 0, 0)
    private val selectedHits = ArrayList<Pair<Item, UiRect>>()
    private val gridHits = ArrayList<Pair<Item, UiRect>>()

    private var searchFocused = false
    private val search = TextFieldLogic(maxLength = 80)
    private var gridScroll = 0
    private var maxGridScroll = 0
    var hoveredStack: ItemStack? = null
        private set

    private var catalog: List<Item> = emptyList()

    fun layout(x: Int, y: Int, w: Int): Int {
        if (w <= 0) {
            card = UiRect(x, y, 0, 0)
            modeHits = emptyList()
            searchBox = UiRect(0, 0, 0, 0)
            handButton = UiRect(0, 0, 0, 0)
            selectedArea = UiRect(0, 0, 0, 0)
            gridArea = UiRect(0, 0, 0, 0)
            return 0
        }
        val modeH = 18
        val searchH = 16
        val selectedH = 24
        val gridH = 84
        val h = modeH + 6 + searchH + 8 + selectedH + 8 + gridH
        card = UiRect(x, y, w, h)
        var cy = y
        val modeW = (w - 8) / 3
        modeHits = ItemFilter.Mode.entries.mapIndexed { index, mode ->
            mode to UiRect(x + index * (modeW + 4), cy, modeW, modeH)
        }
        cy += modeH + 6
        searchBox = UiRect(x, cy, w - 78, searchH)
        handButton = UiRect(searchBox.right() + 6, cy, 72, searchH)
        cy += searchH + 8
        selectedArea = UiRect(x, cy, w, selectedH)
        cy += selectedH + 8
        gridArea = UiRect(x, cy, w, gridH)
        if (catalog.isEmpty()) {
            catalog = Registries.ITEM.filter { it !== Items.AIR }
        }
        return h
    }

    fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        hoveredStack = null
        if (card.w <= 0) {
            return
        }
        val active = ItemFilter.Mode.from(filter.mode)
        for ((mode, hit) in modeHits) {
            val selected = mode == active
            val hovered = hit.contains(mouseX.toDouble(), mouseY.toDouble())
            UiDraw.fill(context, hit, if (selected) HugoTheme.accentSoft else if (hovered) 0x18FFFFFF else HugoTheme.inset)
            UiDraw.border(context, hit.x, hit.y, hit.w, hit.h, if (selected) HugoTheme.accent else HugoTheme.cardBorder)
            val label = UiDraw.ellipsize(textRenderer, mode.label, hit.w - 6)
            context.drawText(
                textRenderer,
                label,
                hit.x + (hit.w - textRenderer.getWidth(label)) / 2,
                hit.y + 5,
                if (selected) HugoTheme.text else HugoTheme.textMuted,
                false
            )
        }

        UiDraw.panel(context, searchBox, HugoTheme.inset, if (searchFocused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = search.text.ifEmpty { "Item suchen…" }
        context.drawText(
            textRenderer,
            UiDraw.ellipsize(textRenderer, shown, searchBox.w - 8),
            searchBox.x + 4,
            searchBox.y + 4,
            if (search.text.isEmpty()) HugoTheme.textDim else HugoTheme.text,
            false
        )

        val handHovered = handButton.contains(mouseX.toDouble(), mouseY.toDouble())
        UiDraw.fill(context, handButton, if (handHovered) HugoTheme.accentSoft else HugoTheme.inset)
        UiDraw.border(context, handButton.x, handButton.y, handButton.w, handButton.h, if (handHovered) HugoTheme.accent else HugoTheme.cardBorder)
        context.drawText(textRenderer, "In Hand", handButton.x + 8, handButton.y + 4, if (handHovered) HugoTheme.text else HugoTheme.textMuted, false)

        drawSelected(context, mouseX, mouseY)
        drawGrid(context, mouseX, mouseY)
    }

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!card.contains(mouseX, mouseY)) {
            searchFocused = false
            return false
        }
        for ((mode, hit) in modeHits) {
            if (hit.contains(mouseX, mouseY)) {
                ItemGlowFilter.setMode(filter, mode)
                onChange()
                searchFocused = false
                return true
            }
        }
        if (searchBox.contains(mouseX, mouseY)) {
            searchFocused = true
            return true
        }
        if (handButton.contains(mouseX, mouseY)) {
            val held = MinecraftClient.getInstance().player?.mainHandStack
            if (held != null && !held.isEmpty) {
                ItemGlowFilter.toggle(filter, held.item)
                onChange()
            }
            searchFocused = false
            return true
        }
        for ((item, hit) in selectedHits) {
            if (hit.contains(mouseX, mouseY)) {
                ItemGlowFilter.toggle(filter, item)
                onChange()
                searchFocused = false
                return true
            }
        }
        for ((item, hit) in gridHits) {
            if (hit.contains(mouseX, mouseY)) {
                ItemGlowFilter.toggle(filter, item)
                onChange()
                searchFocused = false
                return true
            }
        }
        searchFocused = false
        return true
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!gridArea.contains(mouseX, mouseY)) {
            return false
        }
        gridScroll = (gridScroll - (amount * 18).roundToInt()).coerceIn(0, maxGridScroll)
        return true
    }

    fun charTyped(input: CharInput): Boolean {
        if (!searchFocused || !input.isValidChar) {
            return false
        }
        search.insert(input.asString())
        gridScroll = 0
        return true
    }

    fun keyPressed(input: KeyInput): Boolean {
        if (!searchFocused) {
            return false
        }
        if (input.isPaste) {
            search.paste(MinecraftClient.getInstance().keyboard.clipboard)
            gridScroll = 0
            return true
        }
        if (input.isCopy) {
            MinecraftClient.getInstance().keyboard.clipboard = search.copy()
            return true
        }
        when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (search.text.isNotEmpty()) {
                    search.backspace()
                    gridScroll = 0
                }
                return true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                searchFocused = false
                return true
            }
        }
        return false
    }

    fun unfocus() {
        searchFocused = false
    }

    private fun drawSelected(context: DrawContext, mouseX: Int, mouseY: Int) {
        selectedHits.clear()
        UiDraw.panel(context, selectedArea, HugoTheme.inset, HugoTheme.cardBorder)
        val selected = ItemGlowFilter.selectedItems(filter)
        if (selected.isEmpty()) {
            val hint = when (ItemFilter.Mode.from(filter.mode)) {
                ItemFilter.Mode.ALL -> "Alle Items"
                ItemFilter.Mode.WHITELIST -> "Keine Items ausgewählt"
                ItemFilter.Mode.BLACKLIST -> "Keine Ausnahmen"
            }
            context.drawText(textRenderer, hint, selectedArea.x + 6, selectedArea.y + 8, HugoTheme.textDim, false)
            return
        }
        var x = selectedArea.x + 4
        val y = selectedArea.y + 4
        for (item in selected) {
            if (x + 18 > selectedArea.right() - 4) {
                break
            }
            val hit = UiRect(x, y, 16, 16)
            selectedHits += item to hit
            val stack = item.defaultStack
            context.drawItem(stack, x, y)
            if (hit.contains(mouseX.toDouble(), mouseY.toDouble())) {
                hoveredStack = stack
                UiDraw.border(context, x, y, 16, 16, HugoTheme.accent)
            }
            x += 18
        }
        val extra = selected.size - selectedHits.size
        if (extra > 0) {
            context.drawText(textRenderer, "+$extra", x + 2, y + 4, HugoTheme.textMuted, false)
        }
    }

    private fun drawGrid(context: DrawContext, mouseX: Int, mouseY: Int) {
        gridHits.clear()
        UiDraw.panel(context, gridArea, HugoTheme.inset, HugoTheme.cardBorder)
        val query = search.text.trim()
        val items = if (query.isEmpty()) {
            catalog
        } else {
            catalog.filter { item ->
                val id = ItemGlowFilter.idOf(item)
                item.name.string.contains(query, ignoreCase = true) ||
                    id.contains(query, ignoreCase = true)
            }
        }
        val cell = 18
        val cols = (gridArea.w - 6) / cell
        if (cols <= 0) {
            return
        }
        val rows = ceil(items.size / cols.toDouble()).toInt()
        maxGridScroll = (rows * cell - (gridArea.h - 4)).coerceAtLeast(0)
        gridScroll = gridScroll.coerceIn(0, maxGridScroll)

        val startRow = gridScroll / cell
        val visibleRows = gridArea.h / cell + 2
        val selected = filter.items
        for (row in startRow until (startRow + visibleRows).coerceAtMost(rows)) {
            for (col in 0 until cols) {
                val index = row * cols + col
                if (index >= items.size) {
                    break
                }
                val item = items[index]
                val x = gridArea.x + 3 + col * cell
                val y = gridArea.y + 3 + row * cell - gridScroll
                if (y + 16 < gridArea.y || y > gridArea.bottom() - 2) {
                    continue
                }
                val hit = UiRect(x, y, 16, 16)
                gridHits += item to hit
                val stack = item.defaultStack
                if (selected.contains(ItemGlowFilter.idOf(item))) {
                    UiDraw.fill(context, x - 1, y - 1, 18, 18, HugoTheme.accentSoft)
                }
                context.drawItem(stack, x, y)
                if (hit.contains(mouseX.toDouble(), mouseY.toDouble())) {
                    hoveredStack = stack
                    UiDraw.border(context, x, y, 16, 16, HugoTheme.accent)
                }
            }
        }
    }
}
