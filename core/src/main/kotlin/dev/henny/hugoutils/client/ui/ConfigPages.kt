package dev.henny.hugoutils.client.ui

/**
 * Registry for feature modules that want to contribute a tab to [HugoScreen].
 * Pages are registered during mod initialization.
 */
object ConfigPages {
    private val pages = mutableListOf<ConfigPage>()

    fun register(page: ConfigPage) {
        pages.removeAll { it::class == page::class }
        pages += page
    }

    fun forCategory(category: ConfigCategory): List<ConfigPage> =
        pages.filter { it.category == category }

    fun has(category: ConfigCategory): Boolean = pages.any { it.category == category }
}
