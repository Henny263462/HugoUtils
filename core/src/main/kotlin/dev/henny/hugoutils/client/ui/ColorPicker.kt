package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.GlowStyle
import dev.henny.hugoutils.client.config.HsvColor
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.util.math.ColorHelper
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class ColorPicker(
    private val textRenderer: TextRenderer,
    private val onChange: () -> Unit
) {
    var field = UiRect(0, 0, 96, 68)
        private set
    var hueBar = UiRect(0, 0, 12, 68)
        private set
    var brightBar = UiRect(0, 0, 116, 10)
        private set
    var hexBox = UiRect(0, 0, 74, 16)
        private set

    private var draggingField = false
    private var draggingHue = false
    private var draggingBright = false
    private var hexFocused = false
    private var hexBuffer = ""
    private var replaceHexOnType = false

    fun layout(x: Int, y: Int, fieldW: Int, fieldH: Int) {
        val w = fieldW.coerceAtLeast(72)
        val h = fieldH.coerceAtLeast(56)
        field = UiRect(x, y, w, h)
        hueBar = UiRect(x + w + 6, y, 12, h)
        brightBar = UiRect(x, y + h + 6, w + 18, 10)
        hexBox = UiRect(x, brightBar.bottom() + 6, 74, 16)
    }

    fun width(): Int = hueBar.right() - field.x
    fun height(): Int = hexBox.bottom() - field.y

    fun render(context: DrawContext, style: HsvColor) {
        for (col in 0 until field.w) {
            val saturation = col / (field.w - 1).coerceAtLeast(1).toFloat()
            context.fillGradient(
                field.x + col,
                field.y,
                field.x + col + 1,
                field.bottom(),
                rgb(style.hue, saturation, 1f),
                rgb(style.hue, saturation, 0f)
            )
        }
        UiDraw.border(context, field.x, field.y, field.w, field.h, HugoTheme.cardBorder)

        val markerX = field.x + (style.saturation * (field.w - 1)).roundToInt()
        val markerY = field.y + ((1f - style.brightness) * (field.h - 1)).roundToInt()
        context.fill(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xFF000000.toInt())
        context.fill(markerX - 1, markerY - 1, markerX + 2, markerY + 2, 0xFFFFFFFF.toInt())

        for (row in 0 until hueBar.h) {
            val hue = row / (hueBar.h - 1).coerceAtLeast(1).toFloat()
            context.fill(hueBar.x, hueBar.y + row, hueBar.right(), hueBar.y + row + 1, rgb(hue, 1f, 1f))
        }
        UiDraw.border(context, hueBar.x, hueBar.y, hueBar.w, hueBar.h, HugoTheme.cardBorder)
        val hueMarker = hueBar.y + (style.hue * (hueBar.h - 1)).roundToInt()
        context.fill(hueBar.x - 1, hueMarker - 1, hueBar.right() + 1, hueMarker + 2, 0xFFFFFFFF.toInt())

        for (col in 0 until brightBar.w) {
            val value = col / (brightBar.w - 1).coerceAtLeast(1).toFloat()
            context.fill(brightBar.x + col, brightBar.y, brightBar.x + col + 1, brightBar.bottom(), rgb(style.hue, style.saturation, value))
        }
        UiDraw.border(context, brightBar.x, brightBar.y, brightBar.w, brightBar.h, HugoTheme.cardBorder)
        val brightMarker = brightBar.x + (style.brightness * (brightBar.w - 1)).roundToInt()
        context.fill(brightMarker - 1, brightBar.y - 1, brightMarker + 2, brightBar.bottom() + 1, 0xFFFFFFFF.toInt())

        UiDraw.panel(context, hexBox, HugoTheme.inset, if (hexFocused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = if (hexFocused) hexBuffer else style.hex()
        context.drawText(textRenderer, "#$shown", hexBox.x + 4, hexBox.y + 4, HugoTheme.text, false)
        context.drawText(textRenderer, "HEX", hexBox.right() + 6, hexBox.y + 4, HugoTheme.textDim, false)
    }

    fun mouseClicked(style: HsvColor, mouseX: Double, mouseY: Double): Boolean {
        if (hexBox.contains(mouseX, mouseY)) {
            hexFocused = true
            hexBuffer = style.hex()
            replaceHexOnType = true
            return true
        }
        hexFocused = false
        replaceHexOnType = false
        if (field.contains(mouseX, mouseY)) {
            draggingField = true
            applyField(style, mouseX, mouseY)
            onChange()
            return true
        }
        if (hueBar.contains(mouseX, mouseY)) {
            draggingHue = true
            applyHue(style, mouseY)
            onChange()
            return true
        }
        if (brightBar.contains(mouseX, mouseY)) {
            draggingBright = true
            applyBright(style, mouseX)
            onChange()
            return true
        }
        return false
    }

    fun mouseDragged(style: HsvColor, mouseX: Double, mouseY: Double): Boolean {
        if (draggingField) {
            applyField(style, mouseX, mouseY)
            onChange()
            return true
        }
        if (draggingHue) {
            applyHue(style, mouseY)
            onChange()
            return true
        }
        if (draggingBright) {
            applyBright(style, mouseX)
            onChange()
            return true
        }
        return false
    }

    fun mouseReleased(): Boolean {
        val any = draggingField || draggingHue || draggingBright
        draggingField = false
        draggingHue = false
        draggingBright = false
        return any
    }

    fun charTyped(style: HsvColor, input: CharInput): Boolean {
        if (!hexFocused || !input.isValidChar) {
            return false
        }
        val ch = input.asString().uppercase()
        if (ch.length == 1 && (ch[0].isDigit() || ch[0] in 'A'..'F')) {
            if (replaceHexOnType) {
                hexBuffer = ""
                replaceHexOnType = false
            }
            if (hexBuffer.length >= 6) return true
            hexBuffer += ch
            if (hexBuffer.length == 6) {
                style.setFromHex(hexBuffer)
                onChange()
            }
            return true
        }
        return false
    }

    fun keyPressed(style: HsvColor, input: KeyInput): Boolean {
        if (!hexFocused) {
            return false
        }
        when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (replaceHexOnType) {
                    hexBuffer = ""
                    replaceHexOnType = false
                    return true
                }
                if (hexBuffer.isNotEmpty()) {
                    hexBuffer = hexBuffer.dropLast(1)
                }
                return true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (hexBuffer.length == 6 && style.setFromHex(hexBuffer)) {
                    onChange()
                } else {
                    hexBuffer = style.hex()
                }
                hexFocused = false
                replaceHexOnType = false
                return true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                hexFocused = false
                hexBuffer = style.hex()
                replaceHexOnType = false
                return true
            }
        }
        return false
    }

    fun unfocus() {
        hexFocused = false
        replaceHexOnType = false
    }

    fun isFocused(): Boolean = hexFocused

    private fun applyField(style: HsvColor, mouseX: Double, mouseY: Double) {
        style.saturation = ((mouseX - field.x) / (field.w - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        style.brightness = (1.0 - (mouseY - field.y) / (field.h - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    }

    private fun applyHue(style: HsvColor, mouseY: Double) {
        style.hue = ((mouseY - hueBar.y) / (hueBar.h - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    }

    private fun applyBright(style: HsvColor, mouseX: Double) {
        style.brightness = ((mouseX - brightBar.x) / (brightBar.w - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    }

    private fun rgb(h: Float, s: Float, v: Float): Int {
        val c = GlowStyle.hsvToRgb(h, s, v)
        return ColorHelper.getArgb(255, c[0], c[1], c[2])
    }
}
