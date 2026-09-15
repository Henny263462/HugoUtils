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
    override fun layout(screenWidth: Int, screenHeight: Int) {
        frame = UiRect((screenWidth - 300) / 2, (screenHeight - 120) / 2, 300, 120)
        confirm = UiRect(frame.right - 82, frame.bottom - 30, 70, 20)
        cancel = UiRect(frame.right - 160, frame.bottom - 30, 70, 20)
    }
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        UiDraw.panel(context, frame, UiDraw.theme.panel, UiDraw.theme.panelBorder)
        val renderer = MinecraftClient.getInstance().textRenderer
        context.drawText(renderer, title, frame.x + 12, frame.y + 12, UiDraw.theme.text, false)
        context.drawText(renderer, UiDraw.ellipsize(renderer, message, frame.w - 24), frame.x + 12, frame.y + 36, UiDraw.theme.textMuted, false)
        Button(confirmLabel, {}, confirm).render(context, renderer, mouseX, mouseY)
        cancelLabel?.let { Button(it, {}, cancel).render(context, renderer, mouseX, mouseY) }
    }
    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (confirm.contains(mouseX, mouseY)) {
            onConfirm()
            UiOverlays.host.close()
            return true
        }
        if (cancelLabel != null && cancel.contains(mouseX, mouseY)) {
            onCancel()
            UiOverlays.host.close()
            return true
        }
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
    private data class ActiveToast(val toast: Toast, var remaining: Float)
    private val toastQueue = ArrayDeque<ActiveToast>()
    val toasts: List<Toast> get() = toastQueue.map { it.toast }

    fun open(popup: Popup) { popupStack += popup }
    fun close() { if (popupStack.isNotEmpty()) popupStack.removeLast() }
    fun closeAll() { popupStack.clear() }
    fun show(toast: Toast) { toastQueue += ActiveToast(toast, toast.durationSeconds) }
    fun update(deltaSeconds: Float) {
        toastQueue.forEach { it.remaining -= deltaSeconds.coerceAtLeast(0f) }
        while (toastQueue.firstOrNull()?.remaining?.let { it <= 0f } == true) toastQueue.removeFirst()
    }

    fun renderToasts(context: DrawContext, renderer: TextRenderer, screenWidth: Int, screenHeight: Int) {
        toastQueue.takeLast(4).forEachIndexed { index, active ->
            val toast = active.toast
            val width = (renderer.getWidth(toast.message) + 20).coerceAtMost(screenWidth - 16)
            val x = screenWidth - width - 8
            val y = screenHeight - 28 - index * 26
            val accent = when (toast.kind) {
                Toast.Kind.INFO -> UiDraw.theme.accent
                Toast.Kind.SUCCESS -> UiDraw.theme.success
                Toast.Kind.ERROR -> UiDraw.theme.danger
            }
            UiDraw.panel(context, x, y, width, 20, UiDraw.theme.tooltip, accent)
            context.drawText(
                renderer,
                UiDraw.ellipsize(renderer, toast.message, width - 12),
                x + 6,
                y + 6,
                UiDraw.theme.text,
                false
            )
        }
    }
}

object UiOverlays {
    @JvmField val host = PopupHost()
}
