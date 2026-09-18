package dev.henny.hugoutils.api

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ClientApiLogicTest {
    @Test
    fun `begin payload uses playerName and playerUuid`() {
        val body = ClientAuth.beginBody("Steve", "069a79f4-44e9-4726-a5be-fca90e38aaf5")
        assertEquals("Steve", body.get("playerName").asString)
        assertEquals("069a79f4-44e9-4726-a5be-fca90e38aaf5", body.get("playerUuid").asString)
    }

    @Test
    fun `complete requires serverId plus identity`() {
        val body = ClientAuth.completeBody("Steve", "069a79f4-44e9-4726-a5be-fca90e38aaf5", "a".repeat(40))
        assertEquals("a".repeat(40), body.get("serverId").asString)
    }

    @Test
    fun `begin response must contain a 40 hex serverId`() {
        val json = JsonParser.parseString("""{"ok":true,"serverId":"${"b".repeat(40)}","expiresAt":"soon"}""").asJsonObject
        val parsed = ClientAuth.parseBegin(json)
        assertEquals("b".repeat(40), parsed.serverId)
        assertEquals("soon", parsed.expiresAt)
    }

    @Test
    fun `complete response rejects non client tokens`() {
        val json = JsonParser.parseString("""{"token":"hsm_live_nope"}""").asJsonObject
        assertThrows<ClientApiException> { ClientAuth.parseComplete(json) }
    }

    @Test
    fun `complete response accepts hsm_cli tokens`() {
        val json = JsonParser.parseString("""{"token":"hsm_cli_test","expiresAt":"later"}""").asJsonObject
        val parsed = ClientAuth.parseComplete(json)
        assertEquals("hsm_cli_test", parsed.token)
    }

    @Test
    fun `complete response accepts nested session token`() {
        val json = JsonParser.parseString(
            """{"data":{"session":{"token":"hsm_cli_nested","expiresAt":"later"}}}"""
        ).asJsonObject
        val parsed = ClientAuth.parseComplete(json)
        assertEquals("hsm_cli_nested", parsed.token)
        assertEquals("later", parsed.expiresAt)
    }

    @Test
    fun `website login code uses the public alphabet`() {
        assertEquals("AB23CD", ClientAuth.normalizeWebsiteCode("ab23cd"))
        assertEquals("AB23CD", ClientAuth.normalizeWebsiteCode("ab-23 cd!"))
        assertEquals("AB23", ClientAuth.normalizeWebsiteCode("AI0O1B23"))
        assertTrue(ClientAuth.isValidWebsiteCode("AB23CD"))
        assertFalse(ClientAuth.isValidWebsiteCode("AB23CI"))
        assertFalse(ClientAuth.isValidWebsiteCode("AB230D"))
        assertTrue(ClientAuth.isWebsiteCodeChar('a'))
        assertFalse(ClientAuth.isWebsiteCodeChar('I'))
        assertFalse(ClientAuth.isWebsiteCodeChar('0'))
        val challenge = ClientAuth.websiteChallengeBody("AB23CD", "Notch", "069a79f4-44e9-4726-a5be-fca404e38aaf")
        assertEquals("challenge", challenge.get("action").asString)
        assertEquals("AB23CD", challenge.get("code").asString)
        assertEquals("Notch", challenge.get("playerName").asString)
        assertEquals("069a79f4-44e9-4726-a5be-fca404e38aaf", challenge.get("playerUuid").asString)
        assertFalse(challenge.has("accessToken"))
        val complete = ClientAuth.websiteCompleteBody("AB23CD")
        assertEquals("complete", complete.get("action").asString)
        assertEquals("AB23CD", complete.get("code").asString)
        assertFalse(complete.has("playerName"))
        assertTrue(ClientAuth.isClientToken("hsm_cli_abc"))
        assertFalse(ClientAuth.isClientToken("AB23CD"))
    }

    @Test
    fun `website complete returns player identity`() {
        val json = JsonParser.parseString(
            """{"ok":true,"playerName":"Notch","playerUuid":"069a79f4-44e9-4726-a5be-fca404e38aaf"}"""
        ).asJsonObject
        val parsed = ClientAuth.parseWebsiteComplete(json)
        assertEquals("Notch", parsed.first)
        assertEquals("069a79f4-44e9-4726-a5be-fca404e38aaf", parsed.second)
        val failed = JsonParser.parseString("""{"ok":false,"error":"invalid_code"}""").asJsonObject
        assertThrows<ClientApiException> { ClientAuth.parseWebsiteComplete(failed) }
    }

    @Test
    fun `feedback reports parse kinds and replies`() {
        val json = JsonParser.parseString(
            """{"reports":[{"id":"11111111-1111-1111-1111-111111111111","kind":"issue","title":"Lag","body":"Chunks laden langsam.","status":"in_progress","source":"client","reply":"Danke","repliedAt":"now","createdAt":"then","updatedAt":"then"}]}"""
        ).asJsonObject
        val reports = ClientFeedback.parseList(json)
        assertEquals(1, reports.size)
        assertEquals("issue", reports[0].kind)
        assertEquals("Issue", reports[0].kindLabel())
        assertEquals("In Arbeit", reports[0].statusLabel())
        assertEquals("Danke", reports[0].reply)
    }

    @Test
    fun `json view keeps the last ten objects`() {
        val items = (1..15).map { index ->
            JsonParser.parseString("""{"id":"$index","name":"n$index"}""").asJsonObject
        }
        val last = JsonView.last(items, 10)
        assertEquals(10, last.size)
        assertEquals("6", last.first().get("id").asString)
        assertEquals("15", last.last().get("id").asString)
    }

    @Test
    fun `json view extracts nested lists and labels`() {
        val json = JsonParser.parseString(
            """{"items":[{"displayName":"Diamond","price":"12","seller":"Steve"}]}"""
        )
        val items = JsonView.objects(json)
        assertEquals(1, items.size)
        assertTrue(JsonView.label(items.first()).contains("Diamond"))
        assertEquals(12.0, JsonView.number(items.first(), "price"))
    }

    @Test
    fun `json view prefers history arrays for charts`() {
        val json = JsonParser.parseString("""{"average":50,"points":[1,2,3,4]}""")
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), JsonView.numbers(json))
    }

    @Test
    fun `feed key ignores changing prices`() {
        val first = JsonParser.parseString(
            """{"type":"sale","itemId":"diamond","seller":"Steve","createdAt":"now","price":10}"""
        ).asJsonObject
        val changed = JsonParser.parseString(
            """{"type":"sale","itemId":"diamond","seller":"Steve","createdAt":"now","price":20}"""
        ).asJsonObject
        assertEquals(MarketFeedWatcher.eventKey(first), MarketFeedWatcher.eventKey(changed))
    }

    @Test
    fun `session store roundtrips a client token`(@TempDir dir: Path) {
        ClientSessionStore.directoryOverride = dir
        ClientSessionStore.save(ClientSession("hsm_cli_abc", "Steve", "uuid", "soon"))
        ClientSessionStore.load()
        assertEquals("hsm_cli_abc", ClientSessionStore.token())
        ClientSessionStore.clear()
        ClientSessionStore.directoryOverride = null
    }

    @Test
    fun `feature and key names unwrap nested objects`() {
        val json = JsonParser.parseString(
            """{"features":{"market":true,"afk":false,"shop":true},"keys":{"shop":true,"hsm_cli_secret":true}}"""
        ).asJsonObject
        assertEquals(listOf("market", "shop"), JsonView.featureNames(json))
        assertEquals(listOf("shop"), JsonView.keyNames(json))
    }

    @Test
    fun `enabled keys keep dotted paths and ignore false flags`() {
        val nested = JsonParser.parseString(
            """{"keys":{"market":{"afk-bot":true,"hidden":false},"debug":{"inv":true}}}"""
        ).asJsonObject
        val flat = JsonParser.parseString(
            """{"keys":{"market.afk-bot":true,"debug.inv":false}}"""
        ).asJsonObject
        assertTrue(JsonView.keyEnabled(nested, "market.afk-bot"))
        assertTrue(JsonView.keyEnabled(nested, "debug.inv"))
        assertFalse(JsonView.keyEnabled(nested, "market.hidden"))
        assertTrue(JsonView.keyEnabled(flat, "market.afk-bot"))
        assertFalse(JsonView.keyEnabled(flat, "debug.inv"))
        ClientFlags.ingest(nested)
        assertTrue(ClientFlags.has("market.afk-bot"))
        assertTrue(ClientFlags.has("debug.inv"))
        ClientFlags.clear()
        assertFalse(ClientFlags.has("market.afk-bot"))
    }

    @Test
    fun `enabled keys read live api feature maps and key records`() {
        val me = JsonParser.parseString(
            """{"playerName":"Steve","features":{"debug.inv":"true","market.afk-bot":"true","hud.minimap":"off"}}"""
        ).asJsonObject
        val features = JsonParser.parseString(
            """{"features":{"debug.inv":"true","market.afk-bot":""},"keys":[{"key":"debug.inv","value":"true","createdAt":"2026-09-18T00:00:00Z","updatedAt":"2026-09-18T00:00:00Z","createdBy":"admin"}]}"""
        ).asJsonObject
        assertTrue(JsonView.keyEnabled(me, "debug.inv"))
        assertTrue(JsonView.keyEnabled(me, "market.afk-bot"))
        assertFalse(JsonView.keyEnabled(me, "hud.minimap"))
        assertTrue(JsonView.keyEnabled(features, "debug.inv"))
        assertTrue(JsonView.keyEnabled(features, "market.afk-bot"))
        assertFalse(JsonView.enabledKeys(features).contains("key"))
        assertFalse(JsonView.enabledKeys(features).contains("value"))
        assertFalse(JsonView.enabledKeys(features).contains("createdAt"))
        ClientFlags.clear()
        ClientFlags.ingest(features, me)
        assertTrue(ClientFlags.has("debug.inv"))
        assertTrue(ClientFlags.has("market.afk-bot"))
        assertFalse(ClientFlags.has("hud.minimap"))
        ClientFlags.clear()
    }

    @Test
    fun `staff badges and permissions are readable`() {
        val json = JsonParser.parseString(
            """{"staff":true,"superAdmin":true,"admin":true,"permissions":["admin.accounts.read","admin.bot.control"]}"""
        ).asJsonObject
        assertEquals(listOf("Staff", "Super Admin", "Admin"), JsonView.staffBadges(json))
        assertEquals(listOf("admin.accounts.read", "admin.bot.control"), JsonView.permissionNames(json))
    }

    @Test
    fun `plan label prefers named plans then premium flag`() {
        val premium = JsonParser.parseString("""{"premium":true}""").asJsonObject
        val named = JsonParser.parseString("""{"plan":"plus"}""").asJsonObject
        val none = JsonParser.parseString("""{"playerName":"Steve"}""").asJsonObject
        assertEquals("Premium", JsonView.planLabel(premium))
        assertEquals("Plus", JsonView.planLabel(named))
        assertEquals("—", JsonView.planLabel(none))
    }
}
