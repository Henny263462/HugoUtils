package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

class SettingCard(
    private val key: String,
    var title: String,
    var subtitle: String? = null,
    var trailing: String? = null,
    var trailingColor: Int = UiDraw.theme.accent,
    var height: Int = 72
) : SettingBlock {
    override var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set

    override fun layout(x: Int, y: Int, width: Int): Int {
        bounds = UiRect(x, y, width, height)
        return height
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        UiWidgets.hoverCard(context, bounds, mouseX.toDouble(), mouseY.toDouble(), key = key)
        Labels.title(context, renderer, bounds.x + 12, bounds.y + 12, title)
        trailing?.let { Labels.trailing(context, renderer, bounds, it, bounds.y + 12, trailingColor) }
        subtitle?.let { Labels.muted(context, renderer, bounds.x + 12, bounds.y + 30, it, bounds.w - 24) }
    }
}

class SettingHero(
    private val key: String,
    var title: String,
    private val detail: () -> String,
    private val enabled: (() -> Boolean)? = null,
    var action: Button? = null,
    var height: Int = 58
) : SettingBlock {
    override var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set

    override fun layout(x: Int, y: Int, width: Int): Int {
        bounds = UiRect(x, y, width, height)
        action?.let { button ->
            button.bounds = UiRect(bounds.right - 118, bounds.y + 16, 106, 22)
        }
        return height
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        val on = enabled?.invoke()
        UiWidgets.hoverCard(context, bounds, mouseX.toDouble(), mouseY.toDouble(), key = key, selected = on == true)
        Labels.title(context, renderer, bounds.x + 12, bounds.y + 12, title)
        val detailWidth = bounds.w - if (action != null) 140 else 24
        Labels.muted(context, renderer, bounds.x + 12, bounds.y + 30, detail(), detailWidth)
        on?.let { active ->
            Labels.value(
                context,
                renderer,
                bounds.right - renderer.getWidth(if (active) "Aktiv" else "Aus") - 12,
                bounds.y + 12,
                if (active) "Aktiv" else "Aus",
                if (active) UiDraw.theme.success else UiDraw.theme.textDim
            )
        }
        action?.render(context, renderer, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (action?.mouseClicked(mouseX, mouseY) == true) return true
        return bounds.contains(mouseX, mouseY)
    }
}

class SettingNote(
    private val key: String,
    var lines: List<String>,
    var height: Int = 102
) : SettingBlock {
    override var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set

    override fun layout(x: Int, y: Int, width: Int): Int {
        bounds = UiRect(x, y, width, height)
        return height
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        UiWidgets.hoverCard(context, bounds, mouseX.toDouble(), mouseY.toDouble(), key = key)
        lines.forEachIndexed { index, line ->
            Labels.muted(context, renderer, bounds.x + 12, bounds.y + 10 + index * 16, line, bounds.w - 24)
        }
    }
}

class SettingActions(
    @Suppress("unused") private val key: String,
    private val buttons: List<Button>,
    var height: Int = 28
) : SettingBlock {
    override var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set

    override fun layout(x: Int, y: Int, width: Int): Int {
        bounds = UiRect(x, y, width, height)
        if (buttons.isEmpty()) return height
        val gap = 8
        val buttonW = ((width - gap * (buttons.size - 1)) / buttons.size).coerceAtLeast(72)
        buttons.forEachIndexed { index, button ->
            val w = if (index == buttons.lastIndex) width - index * (buttonW + gap) else buttonW
            button.bounds = UiRect(x + index * (buttonW + gap), y, w, 22)
        }
        return height
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        buttons.forEach { it.render(context, renderer, mouseX, mouseY) }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean =
        buttons.any { it.mouseClicked(mouseX, mouseY) }
}

class SettingInfoCard(
    private val key: String,
    var title: String,
    private val badge: () -> String,
    private val badgeColor: () -> Int,
    private val lines: () -> List<Pair<String, Int>>,
    private val progress: () -> Float?,
    private val actions: List<Button>,
    private val accent: () -> Int = { UiDraw.theme.border },
    var height: Int = 118
) : SettingBlock {
    private val bar = ProgressBar()
    override var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set

    override fun layout(x: Int, y: Int, width: Int): Int {
        bounds = UiRect(x, y, width, height)
        val buttonW = ((width - 16 - 16) / 3).coerceAtLeast(78)
        val buttonY = bounds.bottom - 30
        actions.forEachIndexed { index, button ->
            val w = if (index == actions.lastIndex) {
                bounds.right - 8 - (bounds.x + 8 + index * (buttonW + 8))
            } else {
                buttonW
            }
            button.bounds = UiRect(bounds.x + 8 + index * (buttonW + 8), buttonY, w, 20)
        }
        bar.bounds = UiRect(bounds.x + 10, bounds.y + 76, bounds.w - 20, 8)
        return height
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        UiWidgets.hoverCard(context, bounds, mouseX.toDouble(), mouseY.toDouble(), key = key)
        UiDraw.roundedBorder(context, bounds.x, bounds.y, bounds.w, bounds.h, accent(), UiMetrics.CORNER)
        Labels.title(context, renderer, bounds.x + 10, bounds.y + 10, title)
        Labels.value(context, renderer, bounds.x + 10, bounds.y + 26, badge(), badgeColor())
        lines().forEachIndexed { index, (text, color) ->
            Labels.title(
                context,
                renderer,
                bounds.x + 10,
                bounds.y + 44 + index * 16,
                UiDraw.ellipsize(renderer, text, bounds.w - 20),
                color
            )
        }
        val amount = progress()
        bar.visible = amount != null
        if (amount != null) {
            bar.progress = amount
            bar.render(context, renderer, mouseX, mouseY)
        }
        actions.forEach { it.render(context, renderer, mouseX, mouseY) }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean =
        actions.any { it.mouseClicked(mouseX, mouseY) } || bounds.contains(mouseX, mouseY)
}

class SettingListItem(
    private val key: Any,
    var overline: String,
    var overlineColor: Int = UiDraw.theme.accent,
    var trailing: String? = null,
    var trailingColor: Int = UiDraw.theme.textMuted,
    var title: String,
    var detail: String,
    var height: Int = 54
) : SettingBlock {
    override var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set

    override fun layout(x: Int, y: Int, width: Int): Int {
        bounds = UiRect(x, y, width, height)
        return height
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        UiWidgets.hoverCard(context, bounds, mouseX.toDouble(), mouseY.toDouble(), key = key)
        Labels.value(context, renderer, bounds.x + 10, bounds.y + 8, overline, overlineColor)
        trailing?.let { Labels.trailing(context, renderer, bounds, it, bounds.y + 8, trailingColor, 10) }
        Labels.title(context, renderer, bounds.x + 10, bounds.y + 22, UiDraw.ellipsize(renderer, title, bounds.w - 24))
        Labels.muted(context, renderer, bounds.x + 10, bounds.y + 36, detail, bounds.w - 24)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = bounds.contains(mouseX, mouseY)
}
