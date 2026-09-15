package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class ColorPicker(private val textRenderer: TextRenderer, private val onChange: () -> Unit = {}) {
    var field = UiRect(0, 0, 96, 68); private set
    var hueBar = UiRect(0, 0, 12, 68); private set
    var brightBar = UiRect(0, 0, 116, 10); private set
    var hexBox = UiRect(0, 0, 74, 16); private set
    private var draggingField = false
    private var draggingHue = false
    private var draggingBright = false
    private var focused = false
    private var hexBuffer = ""
    private var replaceOnType = false

    fun layout(x: Int, y: Int, fieldW: Int, fieldH: Int) {
        val w = fieldW.coerceAtLeast(72)
        val h = fieldH.coerceAtLeast(56)
        field = UiRect(x, y, w, h)
        hueBar = UiRect(x + w + 6, y, 12, h)
        brightBar = UiRect(x, y + h + 6, w + 18, 10)
        hexBox = UiRect(x, brightBar.bottom + 6, 74, 16)
    }
    fun width() = hueBar.right - field.x
    fun height() = hexBox.bottom - field.y

    fun render(context: DrawContext, color: HsvColor) {
        for (x in 0 until field.w) {
            val s = x / (field.w - 1).coerceAtLeast(1).toFloat()
            context.fillGradient(field.x + x, field.y, field.x + x + 1, field.bottom, argb(color.hue, s, 1f), argb(color.hue, s, 0f))
        }
        UiDraw.border(context, field.x, field.y, field.w, field.h, UiDraw.theme.border)
        val markerX = field.x + (color.saturation * (field.w - 1)).roundToInt()
        val markerY = field.y + ((1f - color.brightness) * (field.h - 1)).roundToInt()
        context.fill(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xFF000000.toInt())
        context.fill(markerX - 1, markerY - 1, markerX + 2, markerY + 2, 0xFFFFFFFF.toInt())
        for (y in 0 until hueBar.h) {
            val hue = y / (hueBar.h - 1).coerceAtLeast(1).toFloat()
            context.fill(hueBar.x, hueBar.y + y, hueBar.right, hueBar.y + y + 1, argb(hue, 1f, 1f))
        }
        UiDraw.border(context, hueBar.x, hueBar.y, hueBar.w, hueBar.h, UiDraw.theme.border)
        val hueMarker = hueBar.y + (color.hue * (hueBar.h - 1)).roundToInt()
        context.fill(hueBar.x - 1, hueMarker - 1, hueBar.right + 1, hueMarker + 2, 0xFFFFFFFF.toInt())
        for (x in 0 until brightBar.w) {
            val value = x / (brightBar.w - 1).coerceAtLeast(1).toFloat()
            context.fill(brightBar.x + x, brightBar.y, brightBar.x + x + 1, brightBar.bottom, argb(color.hue, color.saturation, value))
        }
        UiDraw.border(context, brightBar.x, brightBar.y, brightBar.w, brightBar.h, UiDraw.theme.border)
        val brightMarker = brightBar.x + (color.brightness * (brightBar.w - 1)).roundToInt()
        context.fill(brightMarker - 1, brightBar.y - 1, brightMarker + 2, brightBar.bottom + 1, 0xFFFFFFFF.toInt())
        UiDraw.panel(context, hexBox, UiDraw.theme.inset, if (focused) UiDraw.theme.accent else UiDraw.theme.border)
        context.drawText(textRenderer, "#${if (focused) hexBuffer else Hsv.hex(color)}", hexBox.x + 4, hexBox.y + 4, UiDraw.theme.text, false)
        context.drawText(textRenderer, "HEX", hexBox.right + 6, hexBox.y + 4, UiDraw.theme.textDim, false)
    }

    fun mouseClicked(color: HsvColor, mouseX: Double, mouseY: Double): Boolean {
        if (hexBox.contains(mouseX, mouseY)) {
            focused = true; hexBuffer = Hsv.hex(color); replaceOnType = true; return true
        }
        focused = false
        return when {
            field.contains(mouseX, mouseY) -> { draggingField = true; applyField(color, mouseX, mouseY); changed() }
            hueBar.contains(mouseX, mouseY) -> { draggingHue = true; applyHue(color, mouseY); changed() }
            brightBar.contains(mouseX, mouseY) -> { draggingBright = true; applyBrightness(color, mouseX); changed() }
            else -> false
        }
    }
    fun mouseDragged(color: HsvColor, mouseX: Double, mouseY: Double): Boolean = when {
        draggingField -> { applyField(color, mouseX, mouseY); changed() }
        draggingHue -> { applyHue(color, mouseY); changed() }
        draggingBright -> { applyBrightness(color, mouseX); changed() }
        else -> false
    }
    fun mouseReleased(): Boolean = (draggingField || draggingHue || draggingBright).also {
        draggingField = false; draggingHue = false; draggingBright = false
    }
    fun charTyped(color: HsvColor, input: CharInput): Boolean {
        if (!focused || !input.isValidChar) return false
        val value = input.asString().uppercase()
        if (value.length != 1 || (!value[0].isDigit() && value[0] !in 'A'..'F')) return false
        if (replaceOnType) { hexBuffer = ""; replaceOnType = false }
        if (hexBuffer.length < 6) hexBuffer += value
        if (hexBuffer.length == 6 && Hsv.setFromHex(color, hexBuffer)) onChange()
        return true
    }
    fun keyPressed(color: HsvColor, input: KeyInput): Boolean {
        if (!focused) return false
        when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> if (hexBuffer.isNotEmpty()) { hexBuffer = hexBuffer.dropLast(1); replaceOnType = false }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (hexBuffer.length == 6 && Hsv.setFromHex(color, hexBuffer)) onChange()
                focused = false
            }
            GLFW.GLFW_KEY_ESCAPE -> { focused = false; hexBuffer = Hsv.hex(color) }
            else -> return false
        }
        return true
    }
    fun unfocus() { focused = false; replaceOnType = false }
    fun isFocused() = focused
    private fun changed(): Boolean { onChange(); return true }
    private fun applyField(c: HsvColor, x: Double, y: Double) {
        c.saturation = ((x - field.x) / (field.w - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        c.brightness = (1.0 - (y - field.y) / (field.h - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    }
    private fun applyHue(c: HsvColor, y: Double) { c.hue = ((y - hueBar.y) / (hueBar.h - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) }
    private fun applyBrightness(c: HsvColor, x: Double) { c.brightness = ((x - brightBar.x) / (brightBar.w - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) }
    private fun argb(h: Float, s: Float, v: Float) = Hsv.toRgb(h, s, v).let { net.minecraft.util.math.ColorHelper.getArgb(255, it[0], it[1], it[2]) }
}
