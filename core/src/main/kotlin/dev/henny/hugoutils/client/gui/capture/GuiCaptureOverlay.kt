package dev.henny.hugoutils.client.gui.capture

import dev.henny.hugoutils.client.ui.HugoTheme
import dev.henny.hugoutils.client.ui.UiDraw
import dev.henny.hugoutils.client.ui.UiRect
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW

class GuiCaptureOverlay(
    private val client: MinecraftClient,
    val screen: HandledScreen<*>,
    val snapshot: GuiCaptureSnapshot,
    val draft: GuiCaptureDraft
) {
    private var selectedSlot: Int? = null
    private var focused: Field? = Field.SCREEN_ID
    private var panel = UiRect(0, 0, 0, 0)
    private var save = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private var meta = UiRect(0, 0, 0, 0)
    private val fieldRects = linkedMapOf<Field, UiRect>()
    private var roleRect = UiRect(0, 0, 0, 0)
    private var status = "Snapshot eingefroren"

    fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        val originX = GuiCaptureScreenGeometry.x(screen)
        val originY = GuiCaptureScreenGeometry.y(screen)
        snapshot.slots.forEach { slot ->
            val x = originX + slot.x
            val y = originY + slot.y
            UiDraw.fill(context, x, y, 16, 16, 0xE00C0F16.toInt())
            if (!slot.stack.isEmpty) context.drawItem(slot.stack, x, y)
            val color = when {
                slot.slot == selectedSlot -> HugoTheme.accent
                draft.annotations[slot.slot]?.let(::isAnnotated) == true -> HugoTheme.success
                slot.playerInventory -> 0xFF7A8299.toInt()
                else -> 0x553E8EA3
            }
            UiDraw.border(context, x - 1, y - 1, 18, 18, color)
        }

        layout()
        UiDraw.panel(context, panel, HugoTheme.panel, HugoTheme.panelBorder)
        context.drawText(client.textRenderer, "GUI Capture", panel.x + 8, panel.y + 8, HugoTheme.text, false)
        if (selectedSlot != null) drawButton(context, meta, "Meta", mouseX, mouseY)
        drawButton(context, save, "Export", mouseX, mouseY)
        drawButton(context, close, "×", mouseX, mouseY)

        fieldRects.forEach { (field, rect) ->
            val value = value(field)
            context.drawText(client.textRenderer, field.label, rect.x, rect.y - 9, HugoTheme.textDim, false)
            UiDraw.panel(
                context,
                rect,
                HugoTheme.inset,
                if (focused == field) HugoTheme.accent else HugoTheme.cardBorder
            )
            val shown = UiDraw.ellipsize(
                client.textRenderer,
                if (value.isEmpty()) field.placeholder else value,
                rect.w - 8
            )
            context.drawText(
                client.textRenderer,
                shown,
                rect.x + 4,
                rect.y + 4,
                if (value.isEmpty()) HugoTheme.textDim else HugoTheme.text,
                false
            )
        }

        selectedSlot?.let { slot ->
            val annotation = annotation(slot)
            context.drawText(
                client.textRenderer,
                "Slot $slot",
                roleRect.x,
                roleRect.y - 9,
                HugoTheme.accent,
                false
            )
            UiDraw.panel(context, roleRect, HugoTheme.inset, HugoTheme.accentMuted)
            context.drawText(
                client.textRenderer,
                "Rolle: ${annotation.role.id}  ›",
                roleRect.x + 4,
                roleRect.y + 4,
                HugoTheme.text,
                false
            )
        }
        context.drawText(
            client.textRenderer,
            UiDraw.ellipsize(client.textRenderer, status, panel.w - 16),
            panel.x + 8,
            panel.bottom() - 12,
            HugoTheme.textMuted,
            false
        )
    }

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        layout()
        if (panel.contains(mouseX, mouseY)) {
            if (save.contains(mouseX, mouseY)) {
                export()
                return true
            }
            if (meta.contains(mouseX, mouseY) && selectedSlot != null) {
                selectedSlot = null
                focused = Field.SCREEN_ID
                return true
            }
            if (close.contains(mouseX, mouseY)) {
                GuiCaptureController.stop()
                return true
            }
            if (roleRect.contains(mouseX, mouseY) && selectedSlot != null) {
                val annotation = annotation(selectedSlot!!)
                annotation.role = GuiSlotRole.next(annotation.role)
                return true
            }
            fieldRects.entries.firstOrNull { it.value.contains(mouseX, mouseY) }?.let {
                focused = it.key
                return true
            }
            focused = null
            return true
        }

        val originX = GuiCaptureScreenGeometry.x(screen)
        val originY = GuiCaptureScreenGeometry.y(screen)
        snapshot.slots.firstOrNull {
            mouseX >= originX + it.x - 1 && mouseX < originX + it.x + 17 &&
                mouseY >= originY + it.y - 1 && mouseY < originY + it.y + 17
        }?.let {
            selectedSlot = it.slot
            annotation(it.slot)
            focused = Field.ROLE_LABEL
            status = "Slot ${it.slot} ausgewählt"
            return true
        }
        return false
    }

    fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (focused != null) focused = null else GuiCaptureController.stop()
            return true
        }
        val field = focused ?: return false
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                setValue(field, value(field).dropLast(1))
                true
            }
            GLFW.GLFW_KEY_TAB -> {
                val available = activeFields()
                focused = available[(available.indexOf(field) + 1).mod(available.size)]
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                export()
                true
            }
            else -> false
        }
    }

    fun charTyped(input: CharInput): Boolean {
        val field = focused ?: return false
        if (!input.isValidChar || value(field).length >= field.maxLength) return false
        setValue(field, value(field) + input.asString())
        return true
    }

    private fun export() {
        runCatching { GuiCaptureStore.save(snapshot, draft) }
            .onSuccess { status = "Exportiert: ${it.fileName}" }
            .onFailure { status = "Fehler: ${it.message ?: it.javaClass.simpleName}" }
    }

    private fun annotation(slot: Int): SlotAnnotation = draft.annotations.getOrPut(slot) {
        val frozen = snapshot.slots[slot]
        SlotAnnotation(
            role = when {
                frozen.hotbar -> GuiSlotRole.HOTBAR
                frozen.playerInventory -> GuiSlotRole.PLAYER_INVENTORY
                else -> GuiSlotRole.UNKNOWN
            }
        )
    }

    private fun isAnnotated(value: SlotAnnotation): Boolean =
        value.role != GuiSlotRole.UNKNOWN ||
            value.label.isNotBlank() ||
            value.description.isNotBlank() ||
            value.left.isNotBlank() ||
            value.right.isNotBlank() ||
            value.shiftLeft.isNotBlank()

    private fun layout() {
        val width = (screen.width - 12).coerceIn(210, 264)
        panel = UiRect(screen.width - width - 6, 6, width, screen.height - 12)
        save = UiRect(panel.right() - 70, panel.y + 5, 48, 16)
        close = UiRect(panel.right() - 19, panel.y + 5, 14, 16)
        meta = if (selectedSlot == null) UiRect(0, 0, 0, 0)
        else UiRect(panel.right() - 112, panel.y + 5, 38, 16)
        fieldRects.clear()
        var y = panel.y + 39
        activeFields().forEach { field ->
            fieldRects[field] = UiRect(panel.x + 8, y, panel.w - 16, 16)
            y += 27
        }
        roleRect = if (selectedSlot == null) UiRect(0, 0, 0, 0)
        else UiRect(panel.x + 8, y, panel.w - 16, 16)
    }

    private fun activeFields(): List<Field> =
        if (selectedSlot == null) BASE_FIELDS else SLOT_FIELDS

    private fun value(field: Field): String {
        val annotation = selectedSlot?.let(::annotation)
        return when (field) {
            Field.SCREEN_ID -> draft.screenId
            Field.TITLE_PATTERN -> draft.titlePattern
            Field.PURPOSE -> draft.purpose
            Field.OPEN_TYPE -> draft.openType
            Field.OPEN_DETAIL -> draft.openDetail
            Field.LAYOUT -> draft.layout
            Field.NOTES -> draft.notes
            Field.ITEM_SLOTS -> draft.itemSlots
            Field.ROLE_LABEL -> annotation?.label.orEmpty()
            Field.ROLE_DESCRIPTION -> annotation?.description.orEmpty()
            Field.CLICK_LEFT -> annotation?.left.orEmpty()
            Field.CLICK_RIGHT -> annotation?.right.orEmpty()
            Field.CLICK_SHIFT_LEFT -> annotation?.shiftLeft.orEmpty()
        }
    }

    private fun setValue(field: Field, value: String) {
        val annotation = selectedSlot?.let(::annotation)
        when (field) {
            Field.SCREEN_ID -> draft.screenId = value
            Field.TITLE_PATTERN -> draft.titlePattern = value
            Field.PURPOSE -> draft.purpose = value
            Field.OPEN_TYPE -> draft.openType = value
            Field.OPEN_DETAIL -> draft.openDetail = value
            Field.LAYOUT -> draft.layout = value
            Field.NOTES -> draft.notes = value
            Field.ITEM_SLOTS -> draft.itemSlots = value
            Field.ROLE_LABEL -> annotation?.label = value
            Field.ROLE_DESCRIPTION -> annotation?.description = value
            Field.CLICK_LEFT -> annotation?.left = value
            Field.CLICK_RIGHT -> annotation?.right = value
            Field.CLICK_SHIFT_LEFT -> annotation?.shiftLeft = value
        }
    }

    private fun drawButton(context: DrawContext, rect: UiRect, label: String, mouseX: Int, mouseY: Int) {
        val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
        UiDraw.panel(
            context,
            rect,
            if (hovered) HugoTheme.accentSoft else HugoTheme.inset,
            if (hovered) HugoTheme.accent else HugoTheme.cardBorder
        )
        context.drawText(client.textRenderer, label, rect.x + 4, rect.y + 4, HugoTheme.text, false)
    }

    private enum class Field(val label: String, val placeholder: String = "…", val maxLength: Int = 256) {
        SCREEN_ID("screenId", "z. B. ah.main", 64),
        TITLE_PATTERN("titlePattern"),
        PURPOSE("purpose"),
        OPEN_TYPE("openHow.type", "command / npc / click", 16),
        OPEN_DETAIL("openHow.detail"),
        LAYOUT("layout"),
        NOTES("notes", maxLength = 1024),
        ITEM_SLOTS("itemSlots", "z. B. 0-44", 256),
        ROLE_LABEL("label"),
        ROLE_DESCRIPTION("description", maxLength = 512),
        CLICK_LEFT("clicks.left"),
        CLICK_RIGHT("clicks.right"),
        CLICK_SHIFT_LEFT("clicks.shift_left")
    }

    companion object {
        private val BASE_FIELDS = listOf(
            Field.SCREEN_ID,
            Field.TITLE_PATTERN,
            Field.PURPOSE,
            Field.OPEN_TYPE,
            Field.OPEN_DETAIL,
            Field.LAYOUT,
            Field.NOTES,
            Field.ITEM_SLOTS
        )
        private val SLOT_FIELDS = listOf(
            Field.ROLE_LABEL,
            Field.ROLE_DESCRIPTION,
            Field.CLICK_LEFT,
            Field.CLICK_RIGHT,
            Field.CLICK_SHIFT_LEFT
        )
    }
}
