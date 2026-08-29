package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.update.UpdateManager
import dev.henny.hugoutils.update.UpdateState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import kotlin.math.roundToInt

class UpdateConfigPage : ConfigPage {
    override val category = ConfigCategory.UPDATES
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var autoToggle = UiRect(0, 0, 0, 0)
    private var preToggle = UiRect(0, 0, 0, 0)
    private var checkButton = UiRect(0, 0, 0, 0)
    private var updateButton = UiRect(0, 0, 0, 0)
    private var restartButton = UiRect(0, 0, 0, 0)
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var autoAnim = if (ConfigManager.config.checkUpdatesAutomatically) 1f else 0f
    private var preAnim = if (ConfigManager.config.includePrereleases) 1f else 0f

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 210)
        autoToggle = UiRect(x + width - 42, y + 86, 32, 14)
        preToggle = UiRect(x + width - 42, y + 112, 32, 14)
        val buttonW = ((width - 20 - 8) / 2).coerceAtLeast(90)
        checkButton = UiRect(x + 10, y + 140, buttonW, 22)
        updateButton = UiRect(x + 10 + buttonW + 8, y + 140, buttonW, 22)
        restartButton = UiRect(x + 10, y + 170, width - 20, 22)
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = client.textRenderer
        val snapshot = UpdateManager.status()
        autoAnim = UiDraw.lerp(autoAnim, if (ConfigManager.config.checkUpdatesAutomatically) 1f else 0f, 0.25f)
        preAnim = UiDraw.lerp(preAnim, if (ConfigManager.config.includePrereleases) 1f else 0f, 0.25f)

        UiDraw.panel(context, frame, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, "Updates", frame.x + 10, frame.y + 12, HugoTheme.text, false)
        context.drawText(
            font,
            UiDraw.ellipsize(font, statusLine(snapshot.state, snapshot.message, snapshot.availableVersion), frame.w - 20),
            frame.x + 10,
            frame.y + 32,
            statusColor(snapshot.state),
            false
        )
        snapshot.installedVersion?.let { installed ->
            context.drawText(font, "Installed: v$installed", frame.x + 10, frame.y + 48, HugoTheme.textDim, false)
        }
        if (snapshot.state == UpdateState.DOWNLOADING ||
            snapshot.state == UpdateState.VERIFYING ||
            snapshot.state == UpdateState.INSTALLING
        ) {
            val bar = UiRect(frame.x + 10, frame.y + 64, frame.w - 20, 8)
            UiDraw.fill(context, bar, HugoTheme.inset)
            val fill = ((bar.w - 2) * snapshot.progress.coerceIn(0f, 1f)).roundToInt()
            if (fill > 0) UiDraw.fill(context, bar.x + 1, bar.y + 1, fill, bar.h - 2, HugoTheme.accent)
        }

        context.drawText(font, "Check for updates automatically", frame.x + 10, frame.y + 88, HugoTheme.text, false)
        drawToggle(context, autoToggle, autoAnim)
        context.drawText(font, "Include prerelease versions", frame.x + 10, frame.y + 114, HugoTheme.text, false)
        drawToggle(context, preToggle, preAnim)

        drawButton(context, checkButton, "Check", true)
        drawButton(
            context,
            updateButton,
            "Update",
            snapshot.state == UpdateState.UPDATE_AVAILABLE
        )
        drawButton(
            context,
            restartButton,
            "Minecraft neu starten",
            snapshot.state == UpdateState.PENDING_RESTART
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (autoToggle.contains(mouseX, mouseY) || toggleRow(autoToggle, mouseX, mouseY)) {
            ConfigManager.update { it.checkUpdatesAutomatically = !it.checkUpdatesAutomatically }
            return true
        }
        if (preToggle.contains(mouseX, mouseY) || toggleRow(preToggle, mouseX, mouseY)) {
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

    private fun toggleRow(toggle: UiRect, mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= frame.x && mouseX < toggle.x && mouseY >= toggle.y - 4 && mouseY <= toggle.bottom() + 4
    }

    private fun drawToggle(context: DrawContext, rect: UiRect, anim: Float) {
        val hovered = rect.contains(lastMouseX, lastMouseY)
        val on = HugoTheme.lerpColor(HugoTheme.trackOff, HugoTheme.success, anim)
        UiDraw.fill(context, rect, on)
        UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (hovered) HugoTheme.accent else HugoTheme.cardBorder)
        val knobW = 12
        val knobX = rect.x + 2 + ((rect.w - knobW - 4) * anim).roundToInt()
        UiDraw.fill(context, knobX, rect.y + 2, knobW, rect.h - 4, HugoTheme.knob)
    }

    private fun drawButton(context: DrawContext, rect: UiRect, label: String, enabled: Boolean) {
        val hovered = enabled && rect.contains(lastMouseX, lastMouseY)
        UiDraw.fill(context, rect, when {
            !enabled -> HugoTheme.inset
            hovered -> HugoTheme.accentSoft
            else -> HugoTheme.inset
        })
        UiDraw.border(
            context,
            rect.x,
            rect.y,
            rect.w,
            rect.h,
            if (hovered) HugoTheme.accent else HugoTheme.cardBorder
        )
        val font = client.textRenderer
        val color = if (enabled) HugoTheme.text else HugoTheme.textDim
        context.drawText(
            font,
            UiDraw.ellipsize(font, label, rect.w - 8),
            rect.x + 4,
            rect.y + 7,
            color,
            false
        )
    }

    private fun statusLine(state: UpdateState, message: String?, available: String?): String {
        if (!message.isNullOrBlank()) return message
        return when (state) {
            UpdateState.UNKNOWN -> "Update status unknown"
            UpdateState.CHECKING -> "Checking for updates…"
            UpdateState.UP_TO_DATE -> "HugoUtils is up to date"
            UpdateState.UPDATE_AVAILABLE -> "Update available: ${available ?: ""}".trim()
            UpdateState.DOWNLOADING -> "Downloading update…"
            UpdateState.VERIFYING -> "Verifying checksum…"
            UpdateState.INSTALLING -> "Installing update…"
            UpdateState.PENDING_RESTART -> "Restart Minecraft to load ${available ?: "the update"}"
            UpdateState.ERROR -> "Update check failed"
        }
    }

    private fun statusColor(state: UpdateState): Int = when (state) {
        UpdateState.ERROR -> HugoTheme.danger
        UpdateState.UPDATE_AVAILABLE, UpdateState.PENDING_RESTART -> HugoTheme.accent
        UpdateState.UP_TO_DATE -> HugoTheme.success
        else -> HugoTheme.textMuted
    }
}
