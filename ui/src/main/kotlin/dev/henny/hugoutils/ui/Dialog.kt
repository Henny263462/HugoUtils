package dev.henny.hugoutils.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

open class Dialog(
    val title: String,
    val message: String,
    val confirmLabel: String = "OK",
    val cancelLabel: String? = null,
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {}
) : Popup {
    protected var frame = UiRect(0, 0, 300, 120)
    private var confirm = UiRect(0, 0, 0, 0)
    private var cancel = UiRect(0, 0, 0, 0)
    private val confirmButton = Button(confirmLabel, {
        onConfirm()
        UiOverlays.host.close()
    }, style = ButtonStyle.PRIMARY)
    private val cancelButton = cancelLabel?.let { label ->
        Button(label, {
            onCancel()
            UiOverlays.host.close()
        }, style = ButtonStyle.GHOST)
    }
    private var screenWidth = 0
    private var screenHeight = 0
    private val entrance = AnimatedFloat(0f, .18f, Easing.EASE_OUT).apply { animateTo(1f) }

    override fun layout(screenWidth: Int, screenHeight: Int) {
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        frame = UiRect((screenWidth - 300) / 2, (screenHeight - 120) / 2, 300, 120)
        confirm = UiRect(frame.right - 82, frame.bottom - 30, 70, 20)
        cancel = UiRect(frame.right - 160, frame.bottom - 30, 70, 20)
        confirmButton.bounds = confirm
        cancelButton?.bounds = cancel
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        entrance.update(UiFrame.deltaSeconds)
        val progress = entrance.value
        val animatedFrame = frame.scaleFromCenter(.94f + .06f * progress)
        UiDraw.fill(context, 0, 0, screenWidth, screenHeight, UiDraw.alpha(UiDraw.theme.overlay, progress * .55f))
        UiDraw.shadow(context, animatedFrame, progress)
        UiDraw.panel(context, animatedFrame, UiDraw.alpha(UiDraw.theme.panel, progress), UiDraw.alpha(UiDraw.theme.panelBorder, progress))
        val renderer = MinecraftClient.getInstance().textRenderer
        Labels.title(context, renderer, frame.x + 12, frame.y + 12, title)
        Labels.muted(context, renderer, frame.x + 12, frame.y + 36, UiDraw.ellipsize(renderer, message, frame.w - 24))
        confirmButton.render(context, renderer, mouseX, mouseY)
        cancelButton?.render(context, renderer, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (confirmButton.mouseClicked(mouseX, mouseY)) return true
        if (cancelButton?.mouseClicked(mouseX, mouseY) == true) return true
        return frame.contains(mouseX, mouseY)
    }
}
