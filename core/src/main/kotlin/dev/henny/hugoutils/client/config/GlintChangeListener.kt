package dev.henny.hugoutils.client.config

object GlintChangeListener {
    private val listeners = mutableListOf<() -> Unit>()

    fun register(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun fire() {
        listeners.forEach { it() }
    }
}
