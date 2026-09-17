package dev.henny.hugoutils.blockoverlay

import dev.henny.hugoutils.client.ui.HugoTheme
import dev.henny.hugoutils.ui.TextFieldLogic
import dev.henny.hugoutils.ui.UiDraw
import dev.henny.hugoutils.ui.UiRect
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import org.lwjgl.glfw.GLFW
import kotlin.math.ceil
import kotlin.math.roundToInt

class BlockPickerPanel(
    private val textRenderer: TextRenderer,
    private val onChange: () -> Unit
) {
    var card = UiRect(0, 0, 0, 0)
        private set
    private var searchBox = UiRect(0, 0, 0, 0)
    private var lookButton = UiRect(0, 0, 0, 0)
    private var selectedArea = UiRect(0, 0, 0, 0)
    private var gridArea = UiRect(0, 0, 0, 0)
    private val selectedHits = ArrayList<Pair<String, UiRect>>()
    private val gridHits = ArrayList<Pair<Block, UiRect>>()
    private val search = TextFieldLogic(maxLength = 80)
    private var searchFocused = false
    private var gridScroll = 0
    private var maxGridScroll = 0
    private var catalog: List<Block> = emptyList()
    var hoveredStack: ItemStack? = null
        private set
    var hoveredId: String? = null
        private set
    var clickedId: String? = null
        private set

    fun layout(x: Int, y: Int, w: Int, h: Int): Int {
        val searchH = 20
        val selectedH = 40
        val height = h.coerceAtLeast(searchH + selectedH + 90)
        card = UiRect(x, y, w, height)
        searchBox = UiRect(x, y, w - 82, searchH)
        lookButton = UiRect(searchBox.right + 6, y, 76, searchH)
        selectedArea = UiRect(x, y + searchH + 6, w, selectedH)
        gridArea = UiRect(x, selectedArea.bottom + 6, w, (card.bottom - selectedArea.bottom - 6).coerceAtLeast(64))
        if (catalog.isEmpty()) {
            catalog = Registries.BLOCK.filter { it !== Blocks.AIR }
        }
        return height
    }

    fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        hoveredStack = null
        hoveredId = null
        UiDraw.panel(context, searchBox, HugoTheme.inset, if (searchFocused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = search.text.ifEmpty { "Block suchen…" }
        context.drawText(
            textRenderer,
            UiDraw.ellipsize(textRenderer, shown, searchBox.w - 8),
            searchBox.x + 6,
            searchBox.y + 6,
            if (search.text.isEmpty()) HugoTheme.textDim else HugoTheme.text,
            false
        )
        val lookHovered = lookButton.contains(mouseX.toDouble(), mouseY.toDouble())
        UiDraw.panel(context, lookButton, if (lookHovered) HugoTheme.accentSoft else HugoTheme.inset, if (lookHovered) HugoTheme.accent else HugoTheme.cardBorder)
        context.drawText(textRenderer, "Im Blick", lookButton.x + 10, lookButton.y + 6, if (lookHovered) HugoTheme.text else HugoTheme.textMuted, false)
        drawSelected(context, mouseX, mouseY)
        drawGrid(context, mouseX, mouseY)
    }

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        clickedId = null
        if (!card.contains(mouseX, mouseY)) {
            searchFocused = false
            return false
        }
        if (searchBox.contains(mouseX, mouseY)) {
            searchFocused = true
            return true
        }
        if (lookButton.contains(mouseX, mouseY)) {
            lookedAtBlock()?.let { add(it) }
            searchFocused = false
            return true
        }
        selectedHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (id, _) ->
            clickedId = id
            hoveredId = id
            searchFocused = false
            return true
        }
        gridHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (block, _) ->
            add(Registries.BLOCK.getId(block).toString())
            searchFocused = false
            return true
        }
        searchFocused = false
        return true
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!gridArea.contains(mouseX, mouseY) && !selectedArea.contains(mouseX, mouseY)) return false
        gridScroll = (gridScroll - (amount * 18).roundToInt()).coerceIn(0, maxGridScroll)
        return true
    }

    fun charTyped(input: CharInput): Boolean {
        if (!searchFocused || !input.isValidChar) return false
        search.insert(input.asString())
        gridScroll = 0
        return true
    }

    fun keyPressed(input: KeyInput): Boolean {
        if (!searchFocused) return false
        if (input.isPaste) {
            search.paste(MinecraftClient.getInstance().keyboard.clipboard)
            gridScroll = 0
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

    private fun add(id: String) {
        if (BlockOverlayConfig.add(id) != null) {
            clickedId = id
            hoveredId = id
            onChange()
        }
    }

    private fun lookedAtBlock(): String? {
        val client = MinecraftClient.getInstance()
        val hit = client.crosshairTarget as? BlockHitResult ?: return null
        if (hit.type != HitResult.Type.BLOCK) return null
        val state = client.world?.getBlockState(hit.blockPos) ?: return null
        return Registries.BLOCK.getId(state.block).toString()
    }

    private fun drawSelected(context: DrawContext, mouseX: Int, mouseY: Int) {
        selectedHits.clear()
        UiDraw.panel(context, selectedArea, HugoTheme.inset, HugoTheme.cardBorder)
        val ids = BlockOverlayConfig.keys()
        if (ids.isEmpty()) {
            context.drawText(textRenderer, "Keine Blöcke ausgewählt", selectedArea.x + 6, selectedArea.y + 14, HugoTheme.textDim, false)
            return
        }
        val cell = 18
        val cols = ((selectedArea.w - 8) / cell).coerceAtLeast(1)
        ids.forEachIndexed { index, id ->
            val col = index % cols
            val row = index / cols
            val x = selectedArea.x + 4 + col * cell
            val y = selectedArea.y + 4 + row * cell
            if (y + 16 > selectedArea.bottom - 2) return@forEachIndexed
            val hit = UiRect(x, y, 16, 16)
            selectedHits += id to hit
            val stack = icon(id)
            context.drawItem(stack, x, y)
            if (hit.contains(mouseX.toDouble(), mouseY.toDouble())) {
                hoveredStack = stack
                hoveredId = id
                UiDraw.border(context, x, y, 16, 16, HugoTheme.accent)
            }
        }
    }

    private fun drawGrid(context: DrawContext, mouseX: Int, mouseY: Int) {
        gridHits.clear()
        UiDraw.panel(context, gridArea, HugoTheme.inset, HugoTheme.cardBorder)
        val query = search.text.trim()
        val blocks = if (query.isEmpty()) catalog else catalog.filter { block ->
            val id = Registries.BLOCK.getId(block).toString()
            block.name.string.contains(query, ignoreCase = true) || id.contains(query, ignoreCase = true)
        }
        val cell = 18
        val cols = ((gridArea.w - 8) / cell).coerceAtLeast(1)
        val rows = ceil(blocks.size / cols.toDouble()).toInt()
        maxGridScroll = (rows * cell - (gridArea.h - 6)).coerceAtLeast(0)
        gridScroll = gridScroll.coerceIn(0, maxGridScroll)
        val startRow = gridScroll / cell
        val visibleRows = gridArea.h / cell + 2
        val selected = BlockOverlayConfig.entries.keys
        context.enableScissor(gridArea.x + 1, gridArea.y + 1, gridArea.right - 1, gridArea.bottom - 1)
        for (row in startRow until (startRow + visibleRows).coerceAtMost(rows)) {
            for (col in 0 until cols) {
                val index = row * cols + col
                if (index >= blocks.size) break
                val block = blocks[index]
                val x = gridArea.x + 4 + col * cell
                val y = gridArea.y + 4 + row * cell - gridScroll
                if (y + 16 < gridArea.y || y > gridArea.bottom - 2) continue
                val hit = UiRect(x, y, 16, 16)
                gridHits += block to hit
                val id = Registries.BLOCK.getId(block).toString()
                val stack = icon(id)
                if (id in selected) UiDraw.fill(context, x - 1, y - 1, 18, 18, HugoTheme.accentSoft)
                context.drawItem(stack, x, y)
                if (hit.contains(mouseX.toDouble(), mouseY.toDouble())) {
                    hoveredStack = stack
                    hoveredId = id
                    UiDraw.border(context, x, y, 16, 16, HugoTheme.accent)
                }
            }
        }
        context.disableScissor()
        UiDraw.scrollbar(context, gridArea, gridScroll, maxGridScroll)
    }

    private fun icon(id: String): ItemStack {
        val block = Registries.BLOCK.get(net.minecraft.util.Identifier.tryParse(id) ?: return ItemStack(Items.BARRIER))
        val item = block.asItem()
        return if (item !== Items.AIR) item.defaultStack else ItemStack(Items.BARRIER)
    }
}
