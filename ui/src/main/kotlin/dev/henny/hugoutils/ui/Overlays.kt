package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack

interface Popup {
    fun layout(screenWidth: Int, screenHeight: Int)
    fun render(context: DrawContext, mouseX: Int, mouseY: Int)
    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseReleased() {}
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    fun keyPressed(input: KeyInput): Boolean = false
    fun charTyped(input: CharInput): Boolean = false
    fun hoveredStack(): ItemStack? = null
}

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
        context.drawText(renderer, title, frame.x + 12, frame.y + 12, UiDraw.theme.text, false)
        context.drawText(renderer, UiDraw.ellipsize(renderer, message, frame.w - 24), frame.x + 12, frame.y + 36, UiDraw.theme.textMuted, false)
        confirmButton.render(context, renderer, mouseX, mouseY)
        cancelButton?.render(context, renderer, mouseX, mouseY)
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (confirmButton.mouseClicked(mouseX, mouseY)) return true
        if (cancelButton?.mouseClicked(mouseX, mouseY) == true) return true
        return frame.contains(mouseX, mouseY)
    }
}

data class Toast(val message: String, val durationSeconds: Float = 3f, val kind: Kind = Kind.INFO) {
    enum class Kind { INFO, SUCCESS, ERROR }
}

class PopupHost {
    private val popupStack = ArrayDeque<Popup>()
    val active: Popup? get() = popupStack.lastOrNull()
    val popupCount: Int get() = popupStack.size
    private val toastBand = ToastBand(VISIBLE_TOASTS)
    val toasts: List<Toast> get() = toastBand.toasts

    fun open(popup: Popup) { popupStack += popup }
    fun close() { if (popupStack.isNotEmpty()) popupStack.removeLast() }
    fun closeAll() { popupStack.clear() }
    fun show(toast: Toast) { toastBand.enqueue(toast) }
    fun update(deltaSeconds: Float) { toastBand.update(deltaSeconds) }

    fun renderToasts(context: DrawContext, renderer: TextRenderer, screenWidth: Int, screenHeight: Int) {
        val shift = toastBand.shift.value
        val offset = (shift * TOAST_SLOT_HEIGHT).toInt()
        val items = buildList {
            toastBand.incoming?.let { add(-1 to it) }
            toastBand.displayed.forEachIndexed { index, slot -> add(index to slot) }
            toastBand.outgoing?.let { add(toastBand.displayed.size to it) }
        }
        for ((index, slot) in items) {
            val y = screenHeight - 28 - index * TOAST_SLOT_HEIGHT - offset
            if (y + TOAST_HEIGHT < 0 || y > screenHeight) continue
            val toast = slot.toast
            val width = (renderer.getWidth(toast.message) + 20).coerceAtMost(screenWidth - 16)
            val progress = slot.visibility.value
            val x = screenWidth - width - 8
            val accent = when (toast.kind) {
                Toast.Kind.INFO -> UiDraw.theme.accent
                Toast.Kind.SUCCESS -> UiDraw.theme.success
                Toast.Kind.ERROR -> UiDraw.theme.danger
            }
            UiDraw.shadow(context, UiRect(x, y, width, TOAST_HEIGHT), .5f * progress)
            UiDraw.panel(context, x, y, width, TOAST_HEIGHT, UiDraw.alpha(UiDraw.theme.tooltip, progress), UiDraw.alpha(accent, progress))
            context.drawText(
                renderer,
                UiDraw.ellipsize(renderer, toast.message, width - 12),
                x + 6,
                y + 6,
                UiDraw.alpha(UiDraw.theme.text, progress),
                false
            )
        }
    }

    companion object {
        const val VISIBLE_TOASTS = 10
        private const val TOAST_SLOT_HEIGHT = 26
        private const val TOAST_HEIGHT = 20
    }
}

object UiOverlays {
    @JvmField val host = PopupHost()
}
