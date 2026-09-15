package dev.henny.hugoutils.client.ui

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

    private fun registerCategory(category: ConfigCategory) {
        if (category.visualChild) registerCategory(ConfigCategory.VISUALS)
        registry.register(
            dev.henny.hugoutils.ui.NavigationEntry(
                id = category.id,
                title = category.title,
                parentId = if (category.visualChild) ConfigCategory.VISUALS.id else null,
                footer = category.footer,
                available = category.available,
                order = category.ordinal
            )
        )
    }
}
