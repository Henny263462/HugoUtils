package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.update.UpdateManager
import dev.henny.hugoutils.update.UpdateState
import dev.henny.hugoutils.ui.ButtonStyle
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import kotlin.math.roundToInt

class UpdateConfigPage : ConfigPage {
    override val category = ConfigCategory.UPDATES
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var statusCard = UiRect(0, 0, 0, 0)
    private var checkButton = UiRect(0, 0, 0, 0)
    private var updateButton = UiRect(0, 0, 0, 0)
    private var restartButton = UiRect(0, 0, 0, 0)
    private var autoCard = UiRect(0, 0, 0, 0)
    private var preCard = UiRect(0, 0, 0, 0)
    private var autoToggle = UiRect(0, 0, 0, 0)
    private var preToggle = UiRect(0, 0, 0, 0)
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var autoAnim = if (ConfigManager.config.checkUpdatesAutomatically) 1f else 0f
    private var preAnim = if (ConfigManager.config.includePrereleases) 1f else 0f

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 216)
        statusCard = UiRect(x + 8, y + 8, width - 16, 118)
        val buttonW = ((width - 16 - 16) / 3).coerceAtLeast(78)
        val buttonY = statusCard.bottom() - 30
        checkButton = UiRect(statusCard.x + 8, buttonY, buttonW, 20)
        updateButton = UiRect(checkButton.right() + 8, buttonY, buttonW, 20)
        restartButton = UiRect(updateButton.right() + 8, buttonY, statusCard.right() - 8 - (updateButton.right() + 8), 20)
        val gap = 8
        val cardW = ((width - 16 - gap) / 2).coerceAtLeast(120)
        val settingsY = statusCard.bottom() + 10
        autoCard = UiRect(x + 8, settingsY, cardW, 72)
        preCard = UiRect(autoCard.right() + gap, settingsY, width - 16 - cardW - gap, 72)
        autoToggle = UiRect(autoCard.right() - 42, autoCard.y + 14, 32, 14)
        preToggle = UiRect(preCard.right() - 42, preCard.y + 14, 32, 14)
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = client.textRenderer
        val snapshot = UpdateManager.status()
        autoAnim = UiDraw.lerp(autoAnim, if (ConfigManager.config.checkUpdatesAutomatically) 1f else 0f, 0.25f)
        preAnim = UiDraw.lerp(preAnim, if (ConfigManager.config.includePrereleases) 1f else 0f, 0.25f)

        UiDraw.panel(context, statusCard, HugoTheme.card, statusBorder(snapshot.state))
        context.drawText(font, "Updates", statusCard.x + 10, statusCard.y + 10, HugoTheme.text, false)
        context.drawText(font, stateBadge(snapshot.state), statusCard.x + 10, statusCard.y + 26, statusColor(snapshot.state), false)
        val installed = snapshot.installedVersion?.let { "v$it" } ?: "—"
        val available = snapshot.availableVersion?.let { "v$it" }
        val versionLine = if (!available.isNullOrBlank() && snapshot.state != UpdateState.UP_TO_DATE) {
            "$installed  →  $available"
        } else {
            "Installiert: $installed"
        }
        context.drawText(font, versionLine, statusCard.x + 10, statusCard.y + 44, HugoTheme.text, false)
        context.drawText(
            font,
            UiDraw.ellipsize(font, statusLine(snapshot.state, snapshot.message, snapshot.availableVersion), statusCard.w - 20),
            statusCard.x + 10,
            statusCard.y + 60,
            HugoTheme.textMuted,
            false
        )
        if (snapshot.state == UpdateState.DOWNLOADING ||
            snapshot.state == UpdateState.VERIFYING ||
            snapshot.state == UpdateState.INSTALLING
        ) {
            val bar = UiRect(statusCard.x + 10, statusCard.y + 76, statusCard.w - 20, 8)
            UiDraw.fill(context, bar, HugoTheme.inset)
            val fill = ((bar.w - 2) * snapshot.progress.coerceIn(0f, 1f)).roundToInt()
            if (fill > 0) UiDraw.fill(context, bar.x + 1, bar.y + 1, fill, bar.h - 2, HugoTheme.accent)
        }

        drawButton(context, checkButton, "Prüfen", snapshot.state != UpdateState.CHECKING, ButtonStyle.SECONDARY)
        drawButton(
            context,
            updateButton,
            "Installieren",
            snapshot.state == UpdateState.UPDATE_AVAILABLE,
            ButtonStyle.PRIMARY
        )
        drawButton(
            context,
            restartButton,
            "Neu starten",
            snapshot.state == UpdateState.PENDING_RESTART,
            ButtonStyle.PRIMARY
        )

        drawSettingCard(
            context,
            autoCard,
            autoToggle,
            autoAnim,
            "Automatisch",
            "Beim Start nach Updates suchen."
        )
        drawSettingCard(
            context,
            preCard,
            preToggle,
            preAnim,
            "Vorabversionen",
            "Auch Pre-Releases anbieten."
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (autoToggle.contains(mouseX, mouseY) || autoCard.contains(mouseX, mouseY) && mouseY <= autoCard.y + 32) {
            ConfigManager.update { it.checkUpdatesAutomatically = !it.checkUpdatesAutomatically }
            return true
        }
        if (preToggle.contains(mouseX, mouseY) || preCard.contains(mouseX, mouseY) && mouseY <= preCard.y + 32) {
            ConfigManager.update { it.includePrereleases = !it.includePrereleases }
            UpdateManager.checkForUpdates()
            return true
        }
        if (checkButton.contains(mouseX, mouseY)) {
            UpdateManager.checkForUpdates(autoApply = false)
            return true
        }
        val snapshot = UpdateManager.status()
        if (updateButton.contains(mouseX, mouseY) && snapshot.state == UpdateState.UPDATE_AVAILABLE) {
            UpdateManager.downloadAndInstall()
            return true
        }
        if (restartButton.contains(mouseX, mouseY) && snapshot.state == UpdateState.PENDING_RESTART) {
            UpdateManager.quitToApply()
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun persist() = ConfigManager.requestSave()
    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    override fun keyPressed(input: KeyInput): Boolean = false
    override fun charTyped(input: CharInput): Boolean = false
    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    override fun mouseReleased() {}

    private fun drawSettingCard(
        context: DrawContext,
        rect: UiRect,
        toggle: UiRect,
        anim: Float,
        title: String,
        detail: String
    ) {
        val font = client.textRenderer
        val hovered = rect.contains(lastMouseX, lastMouseY)
        UiDraw.panel(context, rect, if (hovered) HugoTheme.cardHover else HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, title, rect.x + 10, rect.y + 12, HugoTheme.text, false)
        UiWidgets.toggleProgress(context, toggle, anim, lastMouseX, lastMouseY)
        context.drawText(font, UiDraw.ellipsize(font, detail, rect.w - 20), rect.x + 10, rect.y + 42, HugoTheme.textMuted, false)
    }

    private fun drawButton(context: DrawContext, rect: UiRect, label: String, enabled: Boolean, style: ButtonStyle) {
        UiWidgets.button(context, client.textRenderer, rect, label, lastMouseX, lastMouseY, enabled, style)
    }

    private fun stateBadge(state: UpdateState): String = when (state) {
        UpdateState.UNKNOWN -> "Status unbekannt"
        UpdateState.CHECKING -> "Suche nach Updates…"
        UpdateState.UP_TO_DATE -> "Aktuell"
        UpdateState.UPDATE_AVAILABLE -> "Update verfügbar"
        UpdateState.DOWNLOADING -> "Download läuft"
        UpdateState.VERIFYING -> "Prüfung läuft"
        UpdateState.INSTALLING -> "Installation läuft"
        UpdateState.PENDING_RESTART -> "Neustart nötig"
        UpdateState.ERROR -> "Fehler"
    }

    private fun statusLine(state: UpdateState, message: String?, available: String?): String {
        if (!message.isNullOrBlank()) return message
        return when (state) {
            UpdateState.UNKNOWN -> "Noch nicht geprüft."
            UpdateState.CHECKING -> "Release-Infos werden geladen…"
            UpdateState.UP_TO_DATE -> "HugoUtils ist auf dem neuesten Stand."
            UpdateState.UPDATE_AVAILABLE -> "Version ${available ?: ""} steht bereit.".trim()
            UpdateState.DOWNLOADING -> "Update wird heruntergeladen…"
            UpdateState.VERIFYING -> "Checksumme wird geprüft…"
            UpdateState.INSTALLING -> "Update wird installiert…"
            UpdateState.PENDING_RESTART -> "Starte Minecraft neu, um ${available ?: "das Update"} zu laden."
            UpdateState.ERROR -> "Update-Check fehlgeschlagen."
        }
    }

    private fun statusColor(state: UpdateState): Int = when (state) {
        UpdateState.ERROR -> HugoTheme.danger
        UpdateState.UPDATE_AVAILABLE, UpdateState.PENDING_RESTART -> HugoTheme.accent
        UpdateState.UP_TO_DATE -> HugoTheme.success
        else -> HugoTheme.textMuted
    }

    private fun statusBorder(state: UpdateState): Int = when (state) {
        UpdateState.ERROR -> HugoTheme.danger
        UpdateState.UPDATE_AVAILABLE, UpdateState.PENDING_RESTART -> HugoTheme.accent
        UpdateState.UP_TO_DATE -> HugoTheme.success
        else -> HugoTheme.cardBorder
    }
}
