package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

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
