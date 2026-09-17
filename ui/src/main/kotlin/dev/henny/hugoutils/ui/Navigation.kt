package dev.henny.hugoutils.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack

interface UiPage {
    val id: String
    fun layout(x: Int, y: Int, width: Int, height: Int): Int
    fun render(context: DrawContext, mouseX: Int, mouseY: Int)
    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        if (button == 0) mouseClicked(mouseX, mouseY) else false
    fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseReleased() {}
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    fun keyPressed(input: KeyInput): Boolean = false
    fun charTyped(input: CharInput): Boolean = false
    fun resetUi() {}
    fun hoveredStack(): ItemStack? = null
    fun hoveredTooltip(): String? = null
    fun persist() {}
}

data class ConfigSection<T : Any>(val id: String, val value: T, val save: (T) -> Unit = {})

class ConfigRegistry {
    private val sections = linkedMapOf<String, ConfigSection<*>>()
    fun <T : Any> register(section: ConfigSection<T>) {
        require(section.id.isNotBlank()) { "Config section ID must not be blank" }
        sections[section.id] = section
    }
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> section(id: String): ConfigSection<T>? = sections[id] as? ConfigSection<T>
    fun ids(): Set<String> = sections.keys.toSet()
}

data class NavigationEntry(
    val id: String,
    val title: String,
    val parentId: String? = null,
    val footer: Boolean = false,
    val available: Boolean = true,
    val order: Int = 0
)

class NavigationRegistry {
    private val entries = linkedMapOf<String, NavigationEntry>()
    private val pages = linkedMapOf<String, MutableList<UiPage>>()

    @Synchronized
    fun register(entry: NavigationEntry): NavigationEntry {
        require(entry.id.isNotBlank()) { "Navigation ID must not be blank" }
        require(entry.parentId != entry.id) { "An entry cannot parent itself" }
        entries[entry.id] = entry
        return entry
    }

    @Synchronized
    fun registerPage(entry: NavigationEntry, page: UiPage) {
        register(entry)
        registerPage(entry.id, page)
    }

    @Synchronized
    fun registerPage(entryId: String, page: UiPage) {
        require(entries.containsKey(entryId)) { "Unknown navigation entry: $entryId" }
        val target = pages.getOrPut(entryId) { mutableListOf() }
        target.removeAll { it.id == page.id }
        target += page
    }

    fun entry(id: String): NavigationEntry? = entries[id]
    fun entries(): List<NavigationEntry> = entries.values.sortedBy { it.order }
    fun pages(id: String): List<UiPage> = pages[id]?.toList().orEmpty()
    fun roots(includeFooter: Boolean = false): List<NavigationEntry> =
        entries.values.filter { it.parentId == null && (includeFooter || !it.footer) }.sortedBy { it.order }
    fun footerEntries(): List<NavigationEntry> = entries.values.filter { it.footer }.sortedBy { it.order }
    fun children(parentId: String): List<NavigationEntry> =
        entries.values.filter { it.parentId == parentId && !it.footer }.sortedBy { it.order }

    fun descendants(parentId: String): List<NavigationEntry> = buildList {
        children(parentId).forEach { child ->
            add(child)
            addAll(descendants(child.id))
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
        pages.clear()
    }
}

object UiNavigation {
    @JvmField val registry = NavigationRegistry()
}
