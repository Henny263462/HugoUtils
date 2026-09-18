package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.update.UpdateManager
import dev.henny.hugoutils.update.UpdateState
import dev.henny.hugoutils.ui.Button
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.SettingInfoCard
import dev.henny.hugoutils.ui.SettingToggle
import dev.henny.hugoutils.ui.SettingsColumn
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class UpdateConfigPage : ConfigPage {
    override val category = ConfigCategory.UPDATES
    private val client = MinecraftClient.getInstance()
    private val column = SettingsColumn()
    private val checkButton = Button("Prüfen", { UpdateManager.checkForUpdates(autoApply = false) })
    private val updateButton = Button("Installieren", { UpdateManager.downloadAndInstall() }, style = ButtonStyle.PRIMARY)
    private val restartButton = Button("Neu starten", { UpdateManager.quitToApply() }, style = ButtonStyle.PRIMARY)
    private val statusCard: SettingInfoCard = SettingInfoCard(
        key = "updates-status",
        title = "Updates",
        badge = { stateBadge(UpdateManager.status().state) },
        badgeColor = { statusColor(UpdateManager.status().state) },
        lines = {
            val snapshot = UpdateManager.status()
            val installed = snapshot.installedVersion?.let { "v$it" } ?: "—"
            val available = snapshot.availableVersion?.let { "v$it" }
            val versionLine = if (!available.isNullOrBlank() && snapshot.state != UpdateState.UP_TO_DATE) {
                "$installed  →  $available"
            } else {
                "Installiert: $installed"
            }
            listOf(
                versionLine to HugoTheme.text,
                statusLine(snapshot.state, snapshot.message, snapshot.availableVersion) to HugoTheme.textMuted
            )
        },
        progress = {
            val snapshot = UpdateManager.status()
            if (snapshot.state == UpdateState.DOWNLOADING ||
                snapshot.state == UpdateState.VERIFYING ||
                snapshot.state == UpdateState.INSTALLING
            ) snapshot.progress else null
        },
        actions = listOf(checkButton, updateButton, restartButton),
        accent = { statusBorder(UpdateManager.status().state) }
    )
    private val auto = SettingToggle(
        "updates-auto",
        "Automatisch",
        "Beim Start nach Updates suchen.",
        read = { ConfigManager.config.checkUpdatesAutomatically },
        write = { value -> ConfigManager.update { it.checkUpdatesAutomatically = value } }
    )
    private val pre = SettingToggle(
        "updates-pre",
        "Vorabversionen",
        "Auch Pre-Releases anbieten.",
        read = { ConfigManager.config.includePrereleases },
        write = { value ->
            ConfigManager.update { it.includePrereleases = value }
            UpdateManager.checkForUpdates()
        }
    )

    init {
        column.add(statusCard).add(auto).add(pre)
    }

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        return column.layout(x + 8, y + 8, width - 16) + 16
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        val snapshot = UpdateManager.status()
        checkButton.enabled = snapshot.state != UpdateState.CHECKING
        updateButton.enabled = snapshot.state == UpdateState.UPDATE_AVAILABLE
        restartButton.enabled = snapshot.state == UpdateState.PENDING_RESTART
        column.render(context, client.textRenderer, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean =
        column.mouseClicked(mouseX, mouseY)

    override fun persist() = ConfigManager.requestSave()

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
