package dev.henny.hugoutils.client.gui.capture

import com.google.gson.JsonElement
import net.minecraft.item.ItemStack

enum class GuiSlotRole(val id: String) {
    LISTING("listing"),
    FILLER("filler"),
    NAV_NEXT("nav_next"),
    NAV_PREV("nav_prev"),
    PAGE_INDICATOR("page_indicator"),
    REFRESH("refresh"),
    TAB("tab"),
    FILTER("filter"),
    SORT("sort"),
    CONFIRM("confirm"),
    CANCEL("cancel"),
    CLOSE("close"),
    PLAYER_INVENTORY("player_inventory"),
    HOTBAR("hotbar"),
    UNKNOWN("unknown");

    companion object {
        fun next(current: GuiSlotRole): GuiSlotRole = entries[(current.ordinal + 1) % entries.size]
    }
}

data class FrozenGuiSlot(
    val slot: Int,
    val slotId: Int,
    val inventoryIndex: Int,
    val x: Int,
    val y: Int,
    val playerInventory: Boolean,
    val hotbar: Boolean,
    val stack: ItemStack,
    val itemId: String?,
    val displayName: String,
    val lorePlain: List<String>,
    val components: JsonElement?,
    val nbt: JsonElement?
)

data class GuiCaptureSnapshot(
    val capturedAt: String,
    val title: String,
    val titleRaw: JsonElement,
    val type: String?,
    val handlerClass: String,
    val clientScreenClass: String,
    val rows: Int?,
    val slotCount: Int,
    val inventoryStart: Int?,
    val syncId: Int,
    val backgroundWidth: Int,
    val backgroundHeight: Int,
    val slots: List<FrozenGuiSlot>
)

data class SlotAnnotation(
    var role: GuiSlotRole = GuiSlotRole.UNKNOWN,
    var label: String = "",
    var description: String = "",
    var left: String = "",
    var right: String = "",
    var shiftLeft: String = ""
)

data class GuiCaptureDraft(
    var screenId: String,
    var titlePattern: String,
    var purpose: String = "",
    var openType: String = "command",
    var openDetail: String = "",
    var layout: String,
    var notes: String = "",
    var itemSlots: String = "",
    val annotations: MutableMap<Int, SlotAnnotation> = linkedMapOf()
)
