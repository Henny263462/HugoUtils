package dev.henny.hugoutils.blockhighlight

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
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import org.lwjgl.glfw.GLFW
import kotlin.math.ceil
import kotlin.math.roundToInt

class HighlightBlockPicker(
    private val textRenderer: TextRenderer,
    private val onChange: () -> Unit
) {
    var card = UiRect(0, 0, 0, 0)
        private set
    private var searchBox = UiRect(0, 0, 0, 0)
    private var lookButton = UiRect(0, 0, 0, 0)
    private var listArea = UiRect(0, 0, 0, 0)
    private var gridArea = UiRect(0, 0, 0, 0)
    private val listHits = ArrayList<Pair<String, UiRect>>()
    private val gridHits = ArrayList<Pair<Block, UiRect>>()
    private val search = TextFieldLogic(maxLength = 80)
    private var searchFocused = false
    private var listScroll = 0
    private var maxListScroll = 0
    private var gridScroll = 0
    private var maxGridScroll = 0
    private var catalog: List<Block> = emptyList()
    var hoveredStack: ItemStack? = null
        private set
    var clickedId: String? = null
        private set
    var selectedId: String? = null

    fun layout(x: Int, y: Int, w: Int, h: Int): Int {
        val searchH = 20
        val listH = 88.coerceAtMost((h - searchH - 70).coerceAtLeast(36))
        val height = h.coerceAtLeast(searchH + 40)
        card = UiRect(x, y, w, height)
        searchBox = UiRect(x, y, w - 82, searchH)
        lookButton = UiRect(searchBox.right + 6, y, 76, searchH)
        listArea = UiRect(x, y + searchH + 6, w, listH)
        gridArea = UiRect(x, listArea.bottom + 6, w, (card.bottom - listArea.bottom - 6).coerceAtLeast(64))
        if (catalog.isEmpty()) {
            catalog = Registries.BLOCK.filter { it !== Blocks.AIR }
        }
        return height
    }

    fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        hoveredStack = null
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
        drawList(context, mouseX, mouseY)
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
        listHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (id, _) ->
            clickedId = id
            searchFocused = false
            return true
        }
        gridHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (block, _) ->
            add(Registries.BLOCK.getId(block).toString())
            searchFocused = false
            return true
        }
        searchFocused = false
        return false
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        when {
            listArea.contains(mouseX, mouseY) && maxListScroll > 0 -> {
                listScroll = (listScroll - (amount * 18).roundToInt()).coerceIn(0, maxListScroll)
                return true
            }
            gridArea.contains(mouseX, mouseY) && maxGridScroll > 0 -> {
                gridScroll = (gridScroll - (amount * 18).roundToInt()).coerceIn(0, maxGridScroll)
                return true
            }
            else -> return false
        }
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
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                search.text.takeIf { it.isNotBlank() }?.let { add(it) }
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
        val added = BlockHighlightConfig.add(id)
        if (added != null) {
            clickedId = added
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

    private fun drawList(context: DrawContext, mouseX: Int, mouseY: Int) {
        listHits.clear()
        UiDraw.panel(context, listArea, HugoTheme.inset, HugoTheme.cardBorder)
        val ids = BlockHighlightConfig.keys()
        if (ids.isEmpty()) {
            context.drawText(textRenderer, "Keine Blöcke ausgewählt", listArea.x + 8, listArea.y + 12, HugoTheme.textDim, false)
            return
        }
        val rowH = 20
        maxListScroll = (ids.size * rowH - (listArea.h - 6)).coerceAtLeast(0)
        listScroll = listScroll.coerceIn(0, maxListScroll)
        context.enableScissor(listArea.x + 1, listArea.y + 1, listArea.right - 1, listArea.bottom - 1)
        ids.forEachIndexed { index, id ->
            val y = listArea.y + 3 + index * rowH - listScroll
            if (y + rowH < listArea.y || y > listArea.bottom) return@forEachIndexed
            val hit = UiRect(listArea.x + 3, y, listArea.w - 6, rowH - 2)
            listHits += id to hit
            val hovered = hit.contains(mouseX.toDouble(), mouseY.toDouble())
            if (id == selectedId || hovered) {
                UiDraw.fill(context, hit.x, hit.y, hit.w, hit.h, HugoTheme.accentSoft)
            }
            if (id == selectedId) {
                UiDraw.border(context, hit.x, hit.y, hit.w, hit.h, HugoTheme.accent)
            }
            val stack = icon(id)
            context.drawItem(stack, hit.x + 2, hit.y + 1)
            context.drawText(
                textRenderer,
                UiDraw.ellipsize(textRenderer, label(id), hit.w - 24),
                hit.x + 22,
                hit.y + 6,
                HugoTheme.text,
                false
            )
            if (hovered) hoveredStack = stack
        }
        context.disableScissor()
        UiDraw.scrollbar(context, listArea, listScroll, maxListScroll)
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
        val selected = BlockHighlightConfig.entries.keys
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
                    UiDraw.border(context, x, y, 16, 16, HugoTheme.accent)
                }
            }
        }
        context.disableScissor()
        UiDraw.scrollbar(context, gridArea, gridScroll, maxGridScroll)
    }

    private fun label(id: String): String {
        val block = Registries.BLOCK.get(Identifier.tryParse(id) ?: return id)
        return block.name.string
    }

    private fun icon(id: String): ItemStack {
        val block = Registries.BLOCK.get(Identifier.tryParse(id) ?: return ItemStack(Items.BARRIER))
        val item = block.asItem()
        return if (item !== Items.AIR) item.defaultStack else ItemStack(Items.BARRIER)
    }
}
