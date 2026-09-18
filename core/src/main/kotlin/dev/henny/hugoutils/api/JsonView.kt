package dev.henny.hugoutils.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

object JsonView {
    private val listKeys = listOf(
        "items", "results", "listings", "auctions", "orders", "feed", "events",
        "players", "packs", "accounts", "rows", "data", "entries", "logs", "lines",
        "featured", "trending", "spotlight", "chips", "reports", "feedback",
        "messages", "history", "points", "opportunities", "arbitrage", "deals", "jobs", "hits"
    )
    private val titleKeys = listOf(
        "displayName", "display_name", "itemName", "title", "name", "label",
        "message", "text", "player", "playerName", "seller", "id"
    )
    private val detailKeys = listOf(
        "price", "unitPrice", "buyPrice", "sellPrice", "amount", "total",
        "count", "quantity", "qty", "seller", "buyer", "player", "status",
        "type", "source", "time", "createdAt", "updatedAt", "age"
    )

    fun asObject(element: JsonElement?): JsonObject = when (element) {
        is JsonObject -> element
        is JsonArray -> JsonObject().apply { add("items", element) }
        is JsonPrimitive -> JsonObject().apply { add("value", element) }
        else -> JsonObject()
    }

    fun objects(element: JsonElement?): List<JsonObject> {
        val root = element ?: return emptyList()
        if (root is JsonArray) return root.mapNotNull { it as? JsonObject }
        if (root !is JsonObject) return emptyList()
        for (key in listKeys) {
            val child = root.get(key) ?: continue
            val nested = objects(child)
            if (nested.isNotEmpty()) return nested
        }
        return if (root.entrySet().isNotEmpty() && root.entrySet().all { it.value is JsonObject }) {
            root.entrySet().map { (key, value) ->
                (value as JsonObject).deepCopy().apply {
                    if (!has("id") && !has("name")) addProperty("id", key)
                }
            }
        } else emptyList()
    }

    fun objectsNamed(element: JsonElement?, vararg keys: String): List<JsonObject> {
        val root = element as? JsonObject ?: return emptyList()
        for (key in keys) {
            val child = root.get(key) ?: continue
            val nested = objects(child)
            if (nested.isNotEmpty()) return nested
        }
        return emptyList()
    }

    fun child(obj: JsonObject?, vararg keys: String): JsonObject? {
        if (obj == null) return null
        for (key in keys) {
            val value = lookup(obj, key) ?: continue
            if (value is JsonObject) return value
        }
        return null
    }

    fun number(obj: JsonObject?, vararg keys: String): Double? {
        if (obj == null) return null
        for (key in keys) {
            val value = lookup(obj, key) ?: continue
            parseNumber(value)?.let { return it }
        }
        return null
    }

    fun numbers(element: JsonElement?): List<Double> {
        val values = ArrayList<Double>()
        collectNumbers(element, values, 64)
        return values
    }

    fun parseNumber(element: JsonElement?): Double? {
        if (element == null || !element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        if (primitive.isNumber) return primitive.asDouble
        val text = primitive.asString.trim().replace(" ", "").replace(",", ".")
            .replace("$", "").replace("€", "")
        return text.toDoubleOrNull()
    }

    fun str(obj: JsonObject?, vararg keys: String): String? {
        if (obj == null) return null
        for (key in keys) {
            val value = lookup(obj, key) ?: continue
            if (value.isJsonPrimitive) {
                val text = value.asString.trim()
                if (text.isNotEmpty() && text != "null") return text
            }
        }
        return null
    }

    fun lookup(obj: JsonObject, path: String): JsonElement? {
        var current: JsonElement = obj
        for (part in path.split('.')) {
            current = (current as? JsonObject)?.get(part) ?: return null
        }
        return current
    }

    fun label(obj: JsonObject): String {
        val title = titleKeys.firstNotNullOfOrNull { str(obj, it) } ?: "Eintrag"
        val extra = detailKeys.mapNotNull { key ->
            str(obj, key)?.let { value -> if (key == "name" || value == title) null else value }
        }.distinct().take(2)
        return if (extra.isEmpty()) title else "$title  ·  ${extra.joinToString(" · ")}"
    }

    fun lines(obj: JsonObject, limit: Int = 24): List<String> {
        val lines = ArrayList<String>(limit)
        flatten(obj, "", lines, limit)
        return lines
    }

    fun bool(obj: JsonObject?, vararg keys: String): Boolean? {
        if (obj == null) return null
        for (key in keys) {
            val value = lookup(obj, key) ?: continue
            if (!value.isJsonPrimitive) continue
            val primitive = value.asJsonPrimitive
            if (primitive.isBoolean) return primitive.asBoolean
            if (primitive.isNumber) return primitive.asInt != 0
            when (primitive.asString.trim().lowercase()) {
                "true", "yes", "1" -> return true
                "false", "no", "0" -> return false
            }
        }
        return null
    }

    fun featureNames(element: JsonElement?, limit: Int = 24): List<String> {
        val root = asObject(element)
        val source = root.get("features") ?: root.get("featureFlags") ?: root.get("flags") ?: root
        return enabledNames(source, limit).filter { it != "keys" && it != "features" && it != "staff" }
    }

    fun keyNames(element: JsonElement?, limit: Int = 24): List<String> {
        val root = asObject(element)
        val source = lookup(root, "keys") ?: lookup(root, "clientKeys") ?: lookup(root, "client_keys")
            ?: lookup(root, "features") ?: return emptyList()
        return when (source) {
            is JsonObject -> {
                val named = namedFeatureKey(source)
                if (named != null) listOf(named).take(limit)
                else source.entrySet().map { it.key }.filter { !looksSecret(it) }.take(limit)
            }
            else -> {
                val out = LinkedHashSet<String>()
                collectKeyPaths(source, "", out, limit)
                out.filter { !looksSecret(it) }.take(limit)
            }
        }
    }

    fun enabledKeys(element: JsonElement?, limit: Int = 64): Set<String> {
        if (element == null) return emptySet()
        val root = asObject(element)
        val sources = listOfNotNull(
            lookup(root, "features"),
            lookup(root, "keys"),
            lookup(root, "clientKeys"),
            lookup(root, "client_keys"),
        )
        if (sources.isEmpty()) return emptySet()
        val out = LinkedHashSet<String>()
        for (source in sources) {
            collectKeyPaths(source, "", out, limit)
            if (out.size >= limit) break
        }
        return out
    }

    fun keyEnabled(element: JsonElement?, key: String): Boolean {
        val needle = normalizeKey(key)
        return enabledKeys(element).any { normalizeKey(it) == needle }
    }

    fun staffBadges(element: JsonElement?): List<String> {
        val root = asObject(element)
        return buildList {
            if (bool(root, "staff") == true) add("Staff")
            if (bool(root, "superAdmin", "super_admin") == true) add("Super Admin")
            if (bool(root, "admin") == true) add("Admin")
        }
    }

    fun permissionNames(element: JsonElement?, limit: Int = 16): List<String> {
        val root = asObject(element)
        val source = root.get("permissions") ?: root.get("perms") ?: return emptyList()
        return enabledNames(source, limit)
    }

    fun planLabel(obj: JsonObject?): String {
        str(obj, "plan", "tier", "subscription", "subscription.name", "premium.plan", "premium.tier")?.let { return pretty(it) }
        return when (bool(obj, "premium", "isPremium", "subscribed")) {
            true -> "Premium"
            false -> "Free"
            null -> "—"
        }
    }

    fun enabledNames(element: JsonElement?, limit: Int = 24): List<String> {
        val names = ArrayList<String>()
        collectEnabled(element, names, limit)
        return names.distinct()
    }

    fun pretty(raw: String): String {
        val spaced = raw.replace('_', ' ').replace('-', ' ')
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        return spaced.split(' ', '.').filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.lowercase().replaceFirstChar { it.titlecase() } }
    }

    fun looksSecret(text: String): Boolean =
        text.startsWith("hsm_") || (text.length >= 32 && text.all { it.isLetterOrDigit() || it == '-' || it == '_' })

    fun normalizeKey(key: String): String =
        key.trim().lowercase().replace('_', '-').replace(' ', '-')

    private fun collectKeyPaths(element: JsonElement?, prefix: String, out: MutableSet<String>, limit: Int) {
        if (element == null || out.size >= limit) return
        when (element) {
            is JsonArray -> element.forEach { child ->
                when (child) {
                    is JsonPrimitive -> {
                        val text = child.asString.trim()
                        if (text.isNotEmpty() && !looksSecret(text)) out += if (prefix.isEmpty()) text else "$prefix.$text"
                    }
                    else -> collectKeyPaths(child, prefix, out, limit)
                }
            }
            is JsonPrimitive -> {
                if (prefix.isNotEmpty() && primitiveEnabled(element)) out += prefix
                else if (prefix.isEmpty()) {
                    val text = element.asString.trim()
                    if (text.isNotEmpty() && !looksSecret(text) && primitiveEnabled(element)) out += text
                }
            }
            is JsonObject -> {
                val named = namedFeatureKey(element)
                if (named != null) {
                    if (namedFeatureEnabled(element)) {
                        out += if (prefix.isEmpty()) named else "$prefix.$named"
                    }
                    return
                }
                element.entrySet().forEach { (key, value) ->
                    if (out.size >= limit || looksSecret(key)) return@forEach
                    val path = if (prefix.isEmpty()) key else "$prefix.$key"
                    when (value) {
                        is JsonPrimitive -> if (primitiveEnabled(value)) out += path
                        else -> collectKeyPaths(value, path, out, limit)
                    }
                }
            }
        }
    }

    private fun namedFeatureKey(obj: JsonObject): String? {
        val named = str(obj, "key") ?: return null
        return named.takeIf { !looksSecret(it) }
    }

    private fun namedFeatureEnabled(obj: JsonObject): Boolean {
        if (bool(obj, "enabled", "active") == false) return false
        val value = obj.get("value") ?: obj.get("enabled") ?: obj.get("active")
        return value !is JsonPrimitive || primitiveEnabled(value)
    }

    private fun primitiveEnabled(value: JsonPrimitive): Boolean = when {
        value.isBoolean -> value.asBoolean
        value.isNumber -> value.asInt != 0
        else -> when (value.asString.trim().lowercase()) {
            "", "true", "yes", "on", "1" -> true
            "false", "no", "off", "0" -> false
            else -> true
        }
    }

    fun last(items: List<JsonObject>, count: Int = 10): List<JsonObject> =
        if (items.size <= count) items else items.takeLast(count)

    private fun collectEnabled(element: JsonElement?, out: MutableList<String>, limit: Int) {
        if (element == null || out.size >= limit) return
        when (element) {
            is JsonArray -> element.forEach { child ->
                if (child is JsonPrimitive) {
                    val text = child.asString.trim()
                    if (text.isNotEmpty() && !looksSecret(text)) out += text
                } else {
                    collectEnabled(child, out, limit)
                }
            }
            is JsonObject -> element.entrySet().forEach { (key, value) ->
                if (out.size >= limit) return
                if (key.equals("keys", true) || key.equals("token", true)) return@forEach
                when (value) {
                    is JsonPrimitive -> when {
                        value.isBoolean -> if (value.asBoolean) out += key
                        value.isNumber -> if (value.asInt != 0) out += key
                        else -> {
                            val text = value.asString.trim()
                            when {
                                text.equals("true", true) -> out += key
                                text.equals("false", true) || text == "0" -> Unit
                                text.isNotEmpty() && !looksSecret(text) -> out += if (text.equals(key, true)) key else text
                            }
                        }
                    }
                    is JsonObject -> {
                        val enabled = bool(value, "enabled", "active", "value")
                        if (enabled != false) out += (str(value, "name", "label", "id") ?: key)
                    }
                    is JsonArray -> collectEnabled(value, out, limit)
                    else -> Unit
                }
            }
            is JsonPrimitive -> {
                val text = element.asString.trim()
                if (text.isNotEmpty() && !looksSecret(text)) out += text
            }
            else -> Unit
        }
    }

    private fun flatten(element: JsonElement, prefix: String, out: MutableList<String>, limit: Int) {
        if (out.size >= limit) return
        when (element) {
            is JsonPrimitive -> {
                val value = element.asString.trim()
                if (value.isNotEmpty()) out += if (prefix.isEmpty()) value else "$prefix: $value"
            }
            is JsonObject -> element.entrySet().forEach { (key, value) ->
                flatten(value, if (prefix.isEmpty()) key else "$prefix.$key", out, limit)
            }
            is JsonArray -> {
                if (element.size() == 0) return
                if (element.all { it is JsonPrimitive }) {
                    val joined = element.joinToString(", ") { it.asString }
                    out += if (prefix.isEmpty()) joined else "$prefix: $joined"
                } else {
                    out += "${prefix.ifEmpty { "liste" }} (${element.size()})"
                }
            }
            else -> Unit
        }
    }

    private fun collectNumbers(element: JsonElement?, out: MutableList<Double>, limit: Int) {
        if (element == null || out.size >= limit) return
        when (element) {
            is JsonPrimitive -> parseNumber(element)?.let(out::add)
            is JsonArray -> element.forEach { collectNumbers(it, out, limit) }
            is JsonObject -> {
                for (key in listOf("history", "prices", "points", "spark", "series", "values")) {
                    val child = element.get(key) ?: continue
                    val before = out.size
                    collectNumbers(child, out, limit)
                    if (out.size - before >= 2) return
                }
                number(element, "price", "value", "y", "close")?.let(out::add)
                    ?: element.entrySet().forEach { collectNumbers(it.value, out, limit) }
            }
            else -> Unit
        }
    }
}
