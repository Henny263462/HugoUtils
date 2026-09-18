package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW

class TextField(
    val logic: TextFieldLogic = TextFieldLogic(),
    bounds: UiRect = UiRect(0, 0, 100, 20),
    var placeholder: String = "",
    val onChange: (String) -> Unit = {}
) : Control(bounds) {
    var focused = false

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered, focused = focused)
        val t = UiDraw.theme
        val border = Theme.lerpColor(t.border, t.focusRing, maxOf(interaction.hover.value * .45f, interaction.focus.value))
        UiDraw.panel(context, bounds, Theme.lerpColor(t.inset, t.surfacePressed, interaction.focus.value * .5f), border)
        val shown = logic.text.ifEmpty { placeholder }
        logic.selection?.let { selection ->
            val startX = bounds.x + 4 + renderer.getWidth(logic.text.substring(0, selection.first))
            val endX = bounds.x + 4 + renderer.getWidth(logic.text.substring(0, selection.last + 1))
            UiDraw.fill(
                context,
                startX.coerceAtMost(bounds.right - 3),
                bounds.y + 3,
                (endX - startX).coerceAtLeast(1).coerceAtMost(bounds.right - startX - 3),
                bounds.h - 6,
                t.accentSoft
            )
        }
        context.drawText(
            renderer,
            UiDraw.ellipsize(renderer, shown, bounds.w - 8),
            bounds.x + 4,
            bounds.y + (bounds.h - 8) / 2,
            if (logic.text.isEmpty()) t.textDim else t.text,
            false
        )
        if (focused && logic.selection == null && (UiFrame.elapsedSeconds * 2f).toInt() % 2 == 0) {
            val beforeCursor = logic.text.take(logic.cursor)
            val cursorX = (bounds.x + 4 + renderer.getWidth(beforeCursor)).coerceAtMost(bounds.right - 3)
            UiDraw.fill(context, cursorX, bounds.y + 4, 1, bounds.h - 8, t.text)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        focused = bounds.contains(mouseX, mouseY)
        if (focused) interaction.pulse()
        return focused
    }

    fun type(value: String): Boolean {
        if (!focused) return false
        logic.insert(value)
        onChange(logic.text)
        return true
    }

    fun backspace(): Boolean {
        if (!focused) return false
        logic.backspace()
        onChange(logic.text)
        return true
    }

    fun copy(clipboard: (String) -> Unit): Boolean {
        if (!focused) return false
        clipboard(logic.copy())
        return true
    }

    fun paste(clipboard: () -> String): Boolean {
        if (!focused) return false
        logic.paste(clipboard())
        onChange(logic.text)
        return true
    }

    fun keyPressed(input: KeyInput, clipboard: () -> String, setClipboard: (String) -> Unit): Boolean {
        if (!focused) return false
        if (input.isPaste) return paste(clipboard)
        if (input.isCopy) return copy(setClipboard)
        val selecting = input.modifiers() and GLFW.GLFW_MOD_SHIFT != 0
        return when (input.key()) {
            GLFW.GLFW_KEY_A -> {
                if (input.modifiers() and GLFW.GLFW_MOD_CONTROL == 0) return false
                logic.selectAll()
                true
            }
            GLFW.GLFW_KEY_BACKSPACE -> backspace()
            GLFW.GLFW_KEY_DELETE -> {
                logic.delete()
                onChange(logic.text)
                true
            }
            GLFW.GLFW_KEY_LEFT -> {
                logic.moveCursor(-1, selecting)
                true
            }
            GLFW.GLFW_KEY_RIGHT -> {
                logic.moveCursor(1, selecting)
                true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                focused = false
                true
            }
            else -> false
        }
    }

    fun charTyped(input: CharInput): Boolean =
        if (focused && input.isValidChar) type(input.asString()) else false
}
