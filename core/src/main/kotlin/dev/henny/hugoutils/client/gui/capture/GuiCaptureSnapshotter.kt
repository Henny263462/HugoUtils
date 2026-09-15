package dev.henny.hugoutils.client.gui.capture

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtOps
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryOps
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.GenericContainerScreenHandler
import java.time.Instant

object GuiCaptureSnapshotter {
    fun capture(client: MinecraftClient, screen: HandledScreen<*>): GuiCaptureSnapshot {
        val handler = screen.screenHandler
        val slots = handler.slots
        val lookup = client.networkHandler?.registryManager
        val playerInventory = client.player?.inventory
        val inventoryStart = slots.indexOfFirst { isPlayerSlot(it.inventory, playerInventory) }
            .takeIf { it >= 0 }
        val rows = when {
            handler is GenericContainerScreenHandler -> handler.rows
            screen is GenericContainerScreen -> (slots.size - 36).takeIf { it >= 0 && it % 9 == 0 }?.div(9)
            else -> null
        }

        return GuiCaptureSnapshot(
            capturedAt = Instant.now().toString(),
            title = screen.title.string,
            titleRaw = encodeTitle(screen.title, lookup),
            type = runCatching { Registries.SCREEN_HANDLER.getId(handler.type)?.toString() }.getOrNull(),
            handlerClass = handler.javaClass.name,
            clientScreenClass = screen.javaClass.name,
            rows = rows,
            slotCount = slots.size,
            inventoryStart = inventoryStart,
            syncId = handler.syncId,
            backgroundWidth = GuiCaptureScreenGeometry.backgroundWidth(screen),
            backgroundHeight = GuiCaptureScreenGeometry.backgroundHeight(screen),
            slots = slots.mapIndexed { index, slot ->
                val stack = slot.stack.copy()
                val playerSlot = isPlayerSlot(slot.inventory, playerInventory)
                val encoded = encodeStack(stack, lookup)
                FrozenGuiSlot(
                    slot = index,
                    slotId = slot.id,
                    inventoryIndex = slot.index,
                    x = slot.x,
                    y = slot.y,
                    playerInventory = playerSlot,
                    hotbar = playerSlot && slot.index in 0 until PlayerInventory.getHotbarSize(),
                    stack = stack,
                    itemId = stack.takeUnless(ItemStack::isEmpty)
                        ?.let { Registries.ITEM.getId(it.item).toString() },
                    displayName = stack.takeUnless(ItemStack::isEmpty)?.name?.string.orEmpty(),
                    lorePlain = stack.get(DataComponentTypes.LORE)?.lines?.map { it.string }.orEmpty(),
                    components = (encoded as? JsonObject)?.get("components"),
                    nbt = encodeStackNbt(stack, lookup)
                )
            }
        )
    }

    private fun isPlayerSlot(inventory: Any, playerInventory: PlayerInventory?): Boolean =
        inventory is PlayerInventory || (playerInventory != null && inventory === playerInventory)

    private fun encodeTitle(
        title: net.minecraft.text.Text,
        lookup: RegistryWrapper.WrapperLookup?
    ): JsonElement {
        val ops = lookup?.let { RegistryOps.of(JsonOps.INSTANCE, it) } ?: JsonOps.INSTANCE
        return net.minecraft.text.TextCodecs.CODEC.encodeStart(ops, title).result().orElse(JsonNull.INSTANCE)
    }

    private fun encodeStack(stack: ItemStack, lookup: RegistryWrapper.WrapperLookup?): JsonElement? {
        if (stack.isEmpty || lookup == null) return null
        return ItemStack.CODEC.encodeStart(RegistryOps.of(JsonOps.INSTANCE, lookup), stack)
            .result()
            .orElse(null)
    }

    private fun encodeStackNbt(stack: ItemStack, lookup: RegistryWrapper.WrapperLookup?): JsonElement? {
        if (stack.isEmpty || lookup == null) return null
        return ItemStack.CODEC.encodeStart(RegistryOps.of(NbtOps.INSTANCE, lookup), stack)
            .result()
            .map { NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, it) }
            .orElse(null)
    }
}
