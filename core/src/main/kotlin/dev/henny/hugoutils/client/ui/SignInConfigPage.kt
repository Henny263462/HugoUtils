package dev.henny.hugoutils.client.ui

import com.google.gson.JsonObject
import dev.henny.hugoutils.api.AuthApiClient
import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientFlags
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.JsonView
import dev.henny.hugoutils.api.MarketStats
import dev.henny.hugoutils.ui.ButtonStyle
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.PlayerSkinDrawer
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.component.type.ProfileComponent

class SignInConfigPage : ConfigPage {
    override val category = ConfigCategory.SIGN_IN
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var hero = UiRect(0, 0, 0, 0)
    private var head = UiRect(0, 0, 0, 0)
    private var reload = UiRect(0, 0, 0, 0)
    private var signIn = UiRect(0, 0, 0, 0)
    private var statRects = emptyList<UiRect>()
    private var chipFrame = UiRect(0, 0, 0, 0)
    private var status = "Verbinde dein Minecraft-Konto …"
    private var statusError = false
    private var working = false
    private var playerName = ""
    private var balance: Double? = null
    private var plan = ""
    private var features = emptyList<String>()
    private var keys = emptyList<String>()
    private var staffBadges = emptyList<String>()
    private var permissions = emptyList<String>()
    private var stats = LocalPlayerStats.chips()
    private var hoveredTip: String? = null
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var loadedToken: String? = null
    private var lastLiveAt = 0L
    private var lastMe: JsonObject? = null

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val statCols = if (width >= 480) 3 else 2
        val statRows = (stats.size + statCols - 1) / statCols
        val statsH = if (stats.isEmpty()) 0 else 8 + statRows * (STAT_H + 6)
        val featureRows = chipRows(features.size)
        val keyRows = chipRows(keys.size)
        val staffRows = chipRows(staffBadges.size + permissions.size)
        val sections = listOf(features, keys, staffBadges + permissions).count { it.isNotEmpty() }
        val chipsH = (if (sections == 0) 36 else 12 + featureRows + keyRows + staffRows + sections * 18).coerceAtLeast(36)
        val contentH = 8 + HERO_H + 8 + statsH + 8 + chipsH + 20
        val h = contentH.coerceAtLeast(height.coerceAtLeast(240))
        frame = UiRect(x, y, width, h)
        hero = UiRect(x + 8, y + 8, width - 16, HERO_H)
        head = UiRect(hero.x + 12, hero.y + 16, 48, 48)
        signIn = UiRect(hero.right() - 168, hero.y + 16, 96, 22)
        reload = UiRect(hero.right() - 64, hero.y + 16, 48, 22)
        val statsTop = hero.bottom() + 8
        val statW = ((width - 16 - 6 * (statCols - 1)) / statCols).coerceAtLeast(90)
        statRects = stats.mapIndexed { index, _ ->
            val col = index % statCols
            val row = index / statCols
            UiRect(x + 8 + col * (statW + 6), statsTop + row * (STAT_H + 6), statW, STAT_H)
        }
        val chipsTop = statsTop + statsH
        chipFrame = UiRect(x + 8, chipsTop, width - 16, chipsH)
        return frame.h
    }

    override fun resetUi() {
        playerName = sessionName()
        stats = LocalPlayerStats.chips(lastMe)
        AuthApiClient.ensureLoggedIn()
        refreshProfile(false)
    }

    override fun onShown() {
        stats = LocalPlayerStats.chips(lastMe)
        refreshProfile(false)
    }

    override fun hoveredTooltip(): String? = hoveredTip

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        hoveredTip = null
        if (ClientSessionStore.token() != loadedToken) refreshProfile(false)
        else if (ClientSessionStore.hasToken() && !working && System.currentTimeMillis() - lastLiveAt > 15_000L) {
            refreshProfile(true)
        }
        val font = client.textRenderer
        val name = playerName.ifBlank { sessionName() }.ifBlank { "Account" }
        val signedIn = ClientSessionStore.hasToken()

        UiWidgets.hoverCard(context, hero, lastMouseX, lastMouseY, key = "account-hero")
        drawHead(context, head.x, head.y, head.h)
        val nameX = head.right() + 10
        context.matrices.pushMatrix()
        context.matrices.translate(nameX.toFloat(), (hero.y + 16).toFloat())
        context.matrices.scale(1.45f, 1.45f)
        context.drawText(
            font,
            UiDraw.ellipsize(font, name, ((signIn.x - nameX - 12) / 1.45f).toInt().coerceAtLeast(48)),
            0,
            0,
            HugoTheme.text,
            false
        )
        context.matrices.popMatrix()
        val planLabel = plan.ifBlank { if (signedIn) "—" else "Nicht angemeldet" }
        context.drawText(font, planLabel, nameX, hero.y + 38, if (signedIn) HugoTheme.accent else HugoTheme.textDim, false)
        val money = MarketStats.formatMoney(balance)
        val sub = when {
            money != null -> money
            signedIn -> "API verbunden"
            else -> "Website-Code oder Auto-Login"
        }
        context.drawText(font, sub, nameX, hero.y + 52, HugoTheme.textMuted, false)

        UiWidgets.button(
            context, font, signIn, "Anmelden", lastMouseX, lastMouseY,
            enabled = true, style = ButtonStyle.PRIMARY, key = "account-signin"
        )
        if (signIn.contains(lastMouseX, lastMouseY)) hoveredTip = "6-Zeichen-Code von hugo.henny.dev oder hsm_cli_-Token"
        UiWidgets.button(
            context, font, reload, if (working) "…" else "↻", lastMouseX, lastMouseY,
            enabled = !working, key = "account-reload"
        )
        if (reload.contains(lastMouseX, lastMouseY)) hoveredTip = if (working) "Aktualisiert …" else "Profil aktualisieren"

        stats.zip(statRects).forEach { (chip, rect) ->
            ModPageChrome.stat(context, font, rect, chip.label, chip.value, lastMouseX, lastMouseY)
        }

        UiWidgets.hoverCard(context, chipFrame, lastMouseX, lastMouseY, key = "account-chips")
        var textY = chipFrame.y + 10
        val color = if (statusError) HugoTheme.danger else HugoTheme.textDim
        context.drawText(font, UiDraw.ellipsize(font, status, chipFrame.w - 20), chipFrame.x + 10, textY, color, false)
        textY += 16
        if (features.isEmpty() && keys.isEmpty() && staffBadges.isEmpty() && permissions.isEmpty()) {
            context.drawText(
                font,
                if (signedIn) "Keine Features gemeldet." else "Nach der Anmeldung erscheinen Abo, Features und Keys.",
                chipFrame.x + 10,
                textY,
                HugoTheme.textMuted,
                false
            )
        } else {
            textY = drawChipSection(context, "Features", features, textY, HugoTheme.accent)
            textY = drawChipSection(context, "Keys", keys, textY, HugoTheme.success)
            drawChipSection(context, "Staff", staffBadges + permissions, textY, HugoTheme.glintPurple)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (signIn.contains(mouseX, mouseY)) {
            openSignIn()
            return true
        }
        if (reload.contains(mouseX, mouseY) && !working) {
            refreshProfile(true)
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun persist() = Unit

    private fun openSignIn() {
        PopupManager.open(
            WebsiteCodePopup { ok ->
                if (ok) refreshProfile(true)
            }
        )
    }

    private fun refreshProfile(force: Boolean) {
        loadedToken = ClientSessionStore.token()
        playerName = playerName.ifBlank { sessionName() }
        stats = LocalPlayerStats.chips(lastMe)
        if (!ClientSessionStore.hasToken()) {
            status = "Nicht angemeldet. Website-Code auf hugo.henny.dev oder Auto-Login für Market."
            statusError = false
            if (force) AuthApiClient.ensureLoggedIn()
            return
        }
        if (working && force) return
        working = true
        lastLiveAt = System.currentTimeMillis()
        var nextName = playerName
        var nextBalance: Double? = null
        var nextPlan = ""
        var nextFeatures = emptyList<String>()
        var nextKeys = emptyList<String>()
        var nextStaff = emptyList<String>()
        var nextPermissions = emptyList<String>()
        var nextMe: JsonObject? = lastMe
        ClientJobs.submit({ message, error ->
            working = false
            status = message
            statusError = error
            if (!error) {
                playerName = nextName
                balance = nextBalance
                plan = nextPlan
                features = nextFeatures
                keys = nextKeys
                staffBadges = nextStaff
                permissions = nextPermissions
                lastMe = nextMe
                stats = LocalPlayerStats.chips(nextMe)
            } else if (message.contains("anmelden", ignoreCase = true) || message.contains("unauthorized", ignoreCase = true)) {
                AuthApiClient.ensureLoggedIn()
            }
        }) {
            val me = ClientApi.me(force)
            nextMe = me
            val featureJson = runCatching { ClientApi.features(force) }.getOrNull()
            val staff = runCatching { ClientApi.staff(force) }.getOrNull()
            nextName = JsonView.str(me, "playerName", "name", "username") ?: nextName
            nextBalance = JsonView.number(me, "balance", "account.balance", "credits", "money")
            nextPlan = JsonView.planLabel(me)
            val featureSource = featureJson ?: me
            ClientFlags.ingest(featureJson, me)
            nextFeatures = JsonView.featureNames(featureSource).map(JsonView::pretty).take(12)
            nextKeys = JsonView.keyNames(featureSource).map(JsonView::pretty).take(12)
            nextStaff = JsonView.staffBadges(staff ?: me)
            nextPermissions = JsonView.permissionNames(staff ?: me).take(10)
            "Profil geladen."
        }
    }

    private fun drawChipSection(
        context: DrawContext,
        title: String,
        items: List<String>,
        startY: Int,
        accent: Int
    ): Int {
        if (items.isEmpty()) return startY
        val font = client.textRenderer
        context.drawText(font, title, chipFrame.x + 10, startY, HugoTheme.textMuted, false)
        var y = startY + 12
        val cols = 3
        val w = ((chipFrame.w - 28) / cols)
        items.take(12).forEachIndexed { index, label ->
            val col = index % cols
            val row = index / cols
            val rect = UiRect(chipFrame.x + 10 + col * (w + 4), y + row * 20, w, 18)
            val hovered = rect.contains(lastMouseX, lastMouseY)
            UiDraw.panel(context, rect, if (hovered) HugoTheme.accentSoft else HugoTheme.inset, if (hovered) accent else HugoTheme.cardBorder)
            context.drawText(
                font,
                UiDraw.ellipsize(font, label, rect.w - 10),
                rect.x + 5,
                rect.y + 5,
                if (hovered) HugoTheme.text else HugoTheme.textMuted,
                false
            )
            if (hovered) hoveredTip = "$title\n$label"
        }
        return y + chipRows(items.size) + 6
    }

    private fun drawHead(context: DrawContext, x: Int, y: Int, size: Int) {
        val textures = client.player?.skin
            ?: runCatching { client.playerSkinCache.get(ProfileComponent.ofStatic(client.gameProfile)).textures }.getOrNull()
            ?: DefaultSkinHelper.getSkinTextures(client.gameProfile)
        PlayerSkinDrawer.draw(context, textures, x, y, size)
    }

    private fun sessionName(): String =
        ClientSessionStore.current()?.playerName?.takeIf { it.isNotBlank() }
            ?: client.session.username

    private fun chipRows(count: Int, rowH: Int = 20): Int =
        if (count <= 0) 0 else ((count + 2) / 3) * rowH

    companion object {
        private const val HERO_H = 80
        private const val STAT_H = 46
    }
}
