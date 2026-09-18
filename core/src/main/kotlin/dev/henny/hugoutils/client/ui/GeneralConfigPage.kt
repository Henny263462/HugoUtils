package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.rtp.RtpShare
import dev.henny.hugoutils.ui.SettingCorners
import dev.henny.hugoutils.ui.SettingToggle
import dev.henny.hugoutils.ui.SettingsColumn
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class GeneralConfigPage : ConfigPage {
    override val category = ConfigCategory.GENERAL
    private val client = MinecraftClient.getInstance()
    private val column = SettingsColumn()
    private val restore = SettingToggle(
        "general-restore",
        "Letzte Seite",
        "Beim Öffnen genau dort weitermachen, wo du aufgehört hast.",
        read = { ConfigManager.config.restoreLastPage },
        write = { value -> ConfigManager.update { it.restoreLastPage = value } }
    )
    private val outlier = SettingToggle(
        "general-outliers",
        "Ausreißer-Schutz",
        "Troll-Preise bei Auktionen und Orders ausblenden.",
        read = { ConfigManager.config.outlierProtection },
        write = { value ->
            ConfigManager.update { it.outlierProtection = value }
            ClientApi.invalidate()
        }
    )
    private val rtp = SettingToggle(
        "general-rtp",
        "RTP-Punkte teilen",
        "Nur Random-Teleport-Ziele. 10% Premium-Rabatt, umstellbar auf der Website.",
        read = { RtpShare.sharing() },
        write = { value -> RtpShare.setFromUi(value) }
    )
    private val corners = SettingCorners(
        "general-corner",
        read = { ConfigManager.config.uiCornerRadius },
        write = { value -> ConfigManager.setUiCornerRadius(value) }
    )

    init {
        column.add(restore).add(outlier).add(rtp).add(corners)
    }

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val content = column.layout(x + 8, y + 8, width - 16)
        return content + 16
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        column.render(context, client.textRenderer, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean =
        column.mouseClicked(mouseX, mouseY)

    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean =
        column.mouseDragged(mouseX, mouseY)

    override fun mouseReleased() = column.mouseReleased()

    override fun persist() = ConfigManager.requestSave()
}
