package dev.henny.hugoutils.api

import com.google.gson.JsonObject

object ClientFlags {
    @Volatile
    private var keys: Set<String> = emptySet()
    @Volatile
    var listener: (() -> Unit)? = null

    fun has(key: String): Boolean {
        val needle = JsonView.normalizeKey(key)
        if (needle.isEmpty()) return false
        return keys.any { JsonView.normalizeKey(it) == needle }
    }

    fun ingest(vararg bodies: JsonObject?) {
        val next = LinkedHashSet<String>()
        bodies.forEach { body -> next += JsonView.enabledKeys(body) }
        if (next == keys) return
        keys = next
        listener?.invoke()
    }

    fun clear() {
        if (keys.isEmpty()) return
        keys = emptySet()
        listener?.invoke()
    }

    fun refreshFromApi() {
        if (!ClientSessionStore.hasToken()) {
            clear()
            return
        }
        val features = runCatching { ClientApi.features(true) }.getOrNull()
        val me = runCatching { ClientApi.me(false) }.getOrNull()
        ingest(features, me)
    }

    internal fun snapshot(): Set<String> = keys
}
