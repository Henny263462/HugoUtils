package dev.henny.hugoutils.client.config

/** Loose coupling so the core UI can notify the item-glow module about glint color changes. */
object GlintChangeListener {
    private val listeners = mutableListOf<() -> Unit>()

    fun register(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun fire() {
        listeners.forEach { it() }
    }
}
