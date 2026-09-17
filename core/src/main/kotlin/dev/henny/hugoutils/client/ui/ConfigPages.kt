package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.api.ClientFlags

object ConfigPages {
    private val registry get() = dev.henny.hugoutils.ui.UiNavigation.registry
    init {
        ConfigCategory.entries.forEach(::registerCategory)
    }

    fun register(page: ConfigPage) {
        registerCategory(page.category)
        registry.registerPage(page.category.id, page)
    }

    fun forCategory(category: ConfigCategory): List<ConfigPage> =
        registry.pages(category.id).filterIsInstance<ConfigPage>()

    fun has(category: ConfigCategory): Boolean = registry.pages(category.id).isNotEmpty()

    fun forId(id: String): List<dev.henny.hugoutils.ui.UiPage> = registry.pages(id)

    fun register(entry: dev.henny.hugoutils.ui.NavigationEntry, page: dev.henny.hugoutils.ui.UiPage) {
        registry.registerPage(entry, page)
    }

    fun refreshAvailability() {
        ConfigCategory.entries.forEach { category ->
            val existing = registry.entry(category.id) ?: return@forEach
            val required = category.requiredKey()
            val available = category.available && (required == null || ClientFlags.has(required))
            if (existing.available != available) {
                registry.register(existing.copy(available = available))
            }
        }
    }

    private fun registerCategory(category: ConfigCategory) {
        if (category.group == NavGroup.HIDDEN) return
        category.parentId?.let { parentId ->
            ConfigCategory.entries.firstOrNull { it.id == parentId }?.let(::registerCategory)
        }
        val required = category.requiredKey()
        registry.register(
            dev.henny.hugoutils.ui.NavigationEntry(
                id = category.id,
                title = category.title,
                parentId = category.parentId,
                footer = category.footer,
                available = category.available && required == null,
                order = category.ordinal
            )
        )
    }
}
