package dev.henny.hugoutils.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

object ClientApi {
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private val cache = ConcurrentHashMap<String, Cached>()

    fun discovery(): JsonObject = get("/mod/client/v1", auth = false)
    fun begin(playerName: String, playerUuid: String): JsonObject =
        post("/mod/client/v1/auth/begin", ClientAuth.beginBody(playerName, playerUuid), auth = false)
    fun complete(playerName: String, playerUuid: String, serverId: String): JsonObject =
        post("/mod/client/v1/auth/complete", ClientAuth.completeBody(playerName, playerUuid, serverId), auth = false)
    fun loginCode(code: String, playerName: String, playerUuid: String): JsonObject {
        val body = ClientAuth.codeBody(code, playerName, playerUuid)
        return try {
            post("/mod/client/v1/auth/code", body, auth = false)
        } catch (error: ClientApiException) {
            if (error.status != 404) throw error
            post("/mod/client/v1/auth/login", body, auth = false)
        }
    }
    fun revoke() = post("/mod/client/v1/auth/revoke", JsonObject())

    fun me(force: Boolean = false) = cached("me", force) { get("/mod/client/v1/me") }
    fun features(force: Boolean = false) = cached("features", force) { get("/mod/client/v1/me/features") }
    fun staff(force: Boolean = false) = cached("staff", force) { get("/mod/client/v1/me/staff") }
    fun status(force: Boolean = false) = cached("status", force) { get("/mod/client/v1/status") }
    fun home(force: Boolean = false) = cached("home", force) { get("/mod/client/v1/home") }
    fun stats(force: Boolean = false) = cached("stats", force) { get("/mod/client/v1/stats") }
    fun snapshot(force: Boolean = false) = cached("snapshot", force) { get("/mod/client/v1/snapshot") }
    fun feed(force: Boolean = false) = cached("feed", force) { get("/mod/client/v1/feed", mapOf("limit" to "20")) }
    fun search(query: String, force: Boolean = false) =
        cached("search:$query", force) { get("/mod/client/v1/search", mapOf("q" to query)) }
    fun items(query: String = "", page: Int = 1, source: String = "", limit: Int = 24, force: Boolean = false) =
        cached("items:$query:$page:$source:$limit", force) {
            get("/mod/client/v1/items", buildMap {
                if (query.isNotBlank()) put("q", query)
                if (source.isNotBlank()) put("source", source)
                put("page", page.toString())
                put("limit", limit.toString())
            })
        }
    fun item(id: String, force: Boolean = false) =
        cached("item:$id", force) { get("/mod/client/v1/items/${enc(id)}") }
    fun history(id: String, range: String = "24h", source: String = "auction", force: Boolean = false) =
        cached("history:$id:$range:$source", force) {
            get("/mod/client/v1/items/${enc(id)}/history", buildMap {
                put("range", range)
                put("source", source)
            })
        }
    fun listings(
        itemId: String = "",
        source: String = "",
        active: String = "active",
        page: Int = 1,
        limit: Int = 15,
        force: Boolean = false
    ) = cached("listings:$itemId:$source:$active:$page:$limit", force) {
        get("/mod/client/v1/listings", buildMap {
            if (itemId.isNotBlank()) put("itemId", itemId)
            if (source.isNotBlank()) put("source", source)
            if (active.isNotBlank()) put("active", active)
            put("page", page.toString())
            put("limit", limit.toString())
        })
    }
    fun itemAuctions(id: String, page: Int = 1, limit: Int = 5, force: Boolean = false) =
        cached("item-auctions:$id:$page:$limit", force) {
            get("/mod/client/v1/items/${enc(id)}/auctions", mapOf("page" to page.toString(), "limit" to limit.toString()))
        }
    fun itemOrders(id: String, page: Int = 1, limit: Int = 5, force: Boolean = false) =
        cached("item-orders:$id:$page:$limit", force) {
            get("/mod/client/v1/items/${enc(id)}/orders", mapOf("page" to page.toString(), "limit" to limit.toString()))
        }
    fun listing(id: String, force: Boolean = false) =
        cached("listing:$id", force) { get("/mod/client/v1/listings/${enc(id)}") }
    fun arbitrage(force: Boolean = false) =
        cached("arbitrage", force) { get("/mod/client/v1/market/arbitrage") }
    fun players(query: String = "", force: Boolean = false) =
        cached("players:$query", force) {
            get("/mod/client/v1/players", if (query.isBlank()) emptyMap() else mapOf("q" to query))
        }
    fun player(name: String, force: Boolean = false) =
        cached("player:$name", force) { get("/mod/client/v1/players/${enc(name)}") }
    fun analyse(name: String, refresh: Boolean = false): JsonObject {
        val body = JsonObject().apply {
            addProperty("name", name)
            if (refresh) addProperty("refresh", true)
        }
        return post("/mod/client/v1/analyse", body)
    }
    fun analysis(id: String, force: Boolean = false) =
        cached("analysis:$id", force) { get("/mod/client/v1/analyse/${enc(id)}") }
    fun packs(force: Boolean = false) = cached("packs", force) { get("/mod/client/v1/shop/packs") }
    fun ownedPacks(force: Boolean = false) = cached("owned", force) { get("/mod/client/v1/shop/owned") }
    fun pack(id: String, force: Boolean = false) =
        cached("pack:$id", force) { get("/mod/client/v1/shop/packs/${enc(id)}") }
    fun buyPack(id: String): JsonObject = post("/mod/client/v1/shop/packs/${enc(id)}/buy", JsonObject())
    fun downloadPack(id: String): HttpResponse<ByteArray> = sendBytes("GET", "/mod/client/v1/shop/packs/${enc(id)}/download")

    fun fetchTrusted(url: String): ByteArray {
        val trusted = url.startsWith(ApiConfig.API_ORIGIN) || url.startsWith(ApiConfig.HUGO_BOT_ORIGIN)
        if (!trusted) throw ClientApiException(400, "invalid_request", "Download-URL nicht vertrauenswürdig.")
        val auth = url.startsWith(ApiConfig.API_ORIGIN)
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "*/*")
            .header("User-Agent", ApiConfig.USER_AGENT)
            .GET()
        if (auth) {
            val token = ClientSessionStore.token() ?: throw ClientApiException(401, "unauthorized", "Bitte zuerst anmelden.")
            request.header("Authorization", "Bearer $token")
        }
        val response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() !in 200..299) {
            throw failure(response.statusCode(), response.body().toString(StandardCharsets.UTF_8))
        }
        return response.body()
    }

    fun afk(force: Boolean = false) = cached("afk", force) { get("/mod/client/v1/afk") }
    fun afkLive(force: Boolean = false) = cached("afk-live", force) { get("/mod/client/v1/afk/live") }
    fun afkStart(accountId: String? = null): JsonObject =
        post("/mod/client/v1/afk/start", idBody(accountId))
    fun afkStop(accountId: String? = null): JsonObject =
        post("/mod/client/v1/afk/stop", idBody(accountId))
    fun afkAddAccount(): JsonObject = post("/mod/client/v1/afk/accounts", JsonObject())
    fun afkLinkStart(): JsonObject = post("/mod/client/v1/afk/link/start", JsonObject())
    fun afkCommand(command: String, accountId: String? = null): JsonObject {
        val body = idBody(accountId)
        body.addProperty("command", command)
        return post("/mod/client/v1/afk/live/command", body)
    }

    fun invalidate(prefix: String = "") {
        if (prefix.isEmpty()) cache.clear()
        else cache.keys.removeIf { it.startsWith(prefix) }
    }

    fun get(path: String, query: Map<String, String> = emptyMap(), auth: Boolean = true): JsonObject =
        parse(send("GET", path, query, null, auth).body())

    fun post(path: String, body: JsonObject, auth: Boolean = true): JsonObject =
        parse(send("POST", path, emptyMap(), body, auth).body())

    private fun cached(key: String, force: Boolean, ttlMs: Long = 8_000L, loader: () -> JsonObject): JsonObject {
        if (!force) cache[key]?.takeIf { it.fresh(ttlMs) }?.let { return it.body }
        val body = loader()
        val now = System.currentTimeMillis()
        cache[key] = Cached(body, now)
        cache.entries.removeIf { now - it.value.at > CACHE_MAX_AGE_MS }
        if (cache.size > CACHE_MAX_ENTRIES) {
            cache.entries
                .sortedBy { it.value.at }
                .take(cache.size - CACHE_MAX_ENTRIES)
                .forEach { cache.remove(it.key, it.value) }
        }
        return body
    }

    private fun send(
        method: String,
        path: String,
        query: Map<String, String>,
        body: JsonObject?,
        auth: Boolean
    ): HttpResponse<String> {
        val builder = request(method, path, query, auth)
        if (body != null) {
            builder.header("Content-Type", "application/json")
            builder.method(method, HttpRequest.BodyPublishers.ofString(body.toString()))
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody())
        }
        val response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 401 && auth) ClientSessionStore.clear()
        if (response.statusCode() !in 200..299) throw failure(response.statusCode(), response.body())
        return response
    }

    private fun sendBytes(method: String, path: String): HttpResponse<ByteArray> {
        val response = http.send(
            request(method, path, emptyMap(), true).method(method, HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofByteArray()
        )
        if (response.statusCode() == 401) ClientSessionStore.clear()
        if (response.statusCode() !in 200..299) {
            throw failure(response.statusCode(), response.body().toString(StandardCharsets.UTF_8))
        }
        return response
    }

    private fun request(method: String, path: String, query: Map<String, String>, auth: Boolean): HttpRequest.Builder {
        val uri = URI.create(ApiConfig.API_ORIGIN + path + queryString(query))
        val builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "application/json")
            .header("User-Agent", ApiConfig.USER_AGENT)
        if (auth) {
            val token = ClientSessionStore.token() ?: throw ClientApiException(401, "unauthorized", "Bitte zuerst anmelden.")
            builder.header("Authorization", "Bearer $token")
        }
        return builder
    }

    private fun queryString(query: Map<String, String>): String {
        if (query.isEmpty()) return ""
        return query.entries.joinToString("&", prefix = "?") { (key, value) ->
            "${enc(key)}=${enc(value)}"
        }
    }

    private fun parse(raw: String): JsonObject {
        val element = JsonParser.parseString(raw.ifBlank { "{}" })
        return JsonView.asObject(element)
    }

    private fun failure(status: Int, raw: String): ClientApiException {
        val json = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull()
        val error = JsonView.str(json, "error") ?: "http_$status"
        val message = JsonView.str(json, "message")
            ?: ClientAuth.userMessage(error, "Anfrage fehlgeschlagen (HTTP $status).")
        return ClientApiException(status, error, message)
    }

    private fun idBody(accountId: String?): JsonObject = JsonObject().apply {
        if (!accountId.isNullOrBlank()) {
            addProperty("accountId", accountId)
            addProperty("id", accountId)
        }
    }

    private fun enc(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private data class Cached(val body: JsonObject, val at: Long) {
        fun fresh(ttlMs: Long) = System.currentTimeMillis() - at < ttlMs
    }

    private const val CACHE_MAX_ENTRIES = 48
    private const val CACHE_MAX_AGE_MS = 60_000L
}
