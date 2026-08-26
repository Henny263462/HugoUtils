package dev.henny.hugoutils.client.config

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object ItemGlowFilter {
    @JvmStatic
    fun allows(filter: ItemFilter, stack: ItemStack): Boolean =
        !stack.isEmpty && allows(filter, stack.item)

    @JvmStatic
    fun allows(filter: ItemFilter, item: Item?): Boolean {
        if (item == null || item === Items.AIR) {
            return false
        }
        val id = idOf(item)
        return when (ItemFilter.Mode.from(filter.mode)) {
            ItemFilter.Mode.ALL -> true
            ItemFilter.Mode.WHITELIST -> filter.items.contains(id)
            ItemFilter.Mode.BLACKLIST -> !filter.items.contains(id)
        }
    }

    @JvmStatic
    fun idOf(item: Item): String = Registries.ITEM.getId(item).toString()

    @JvmStatic
    fun itemOf(id: String): Item? {
        val identifier = Identifier.tryParse(id) ?: return null
        val item = Registries.ITEM.get(identifier)
        return item.takeUnless { it === Items.AIR }
    }

    fun toggle(filter: ItemFilter, item: Item) {
        val id = idOf(item)
        if (filter.items.contains(id)) {
            filter.items.remove(id)
        } else {
            filter.items.add(id)
            filter.items.sort()
        }
    }

    fun setMode(filter: ItemFilter, mode: ItemFilter.Mode) {
        filter.mode = mode.id
    }

    fun selectedItems(filter: ItemFilter): List<Item> =
        filter.items.mapNotNull(::itemOf)
}
