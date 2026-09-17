package cn.noryea.fastitems

import cn.noryea.fastitems.config.FastItemsConfig
import dev.henny.hugoutils.client.ui.ConfigCategory
import dev.henny.hugoutils.client.ui.ConfigPage
import dev.henny.hugoutils.client.ui.HugoTheme
import dev.henny.hugoutils.client.ui.ItemFilterPopup
import dev.henny.hugoutils.client.ui.ModPageChrome
import dev.henny.hugoutils.client.ui.PopupManager
import dev.henny.hugoutils.ui.UiRect
import dev.henny.hugoutils.ui.UiWidgets
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack

class FastItemsConfigPage : ConfigPage {
    override val category: ConfigCategory = ConfigCategory.FAST_ITEMS
    private var hero = UiRect(0, 0, 0, 0)
    private var heroToggle = UiRect(0, 0, 0, 0)
    private var filterCard = UiRect(0, 0, 0, 0)
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    private data class Option(
        val label: String,
        val description: String,
        val getter: () -> Boolean,
        val onChange: (Boolean) -> Unit,
        var rect: UiRect = UiRect(0, 0, 0, 0),
        var toggle: UiRect = UiRect(0, 0, 0, 0)
    )

    private val options = listOf(
        Option(
            label = "Schatten",
            description = "Dropped Items werfen weiter einen Schatten.",
            getter = { FastItemsConfig.castShadows },
            onChange = { FastItemsConfig.castShadows = it }
        ),
        Option(
            label = "Seiten",
            description = "Seitliche Flächen von Block-Items zeichnen.",
            getter = { FastItemsConfig.renderSidesOfItems },
            onChange = { FastItemsConfig.renderSidesOfItems = it }
        ),
        Option(
            label = "3D-Modelle",
            description = "Auch Werkzeuge und 3D-Modelle vereinfachen.",
            getter = { FastItemsConfig.affect3DModels },
            onChange = { FastItemsConfig.affect3DModels = it }
        )
    )

    override fun hoveredStack(): ItemStack? = null

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        hero = UiRect(x + 8, y + 8, width - 16, 58)
        heroToggle = UiRect(hero.right() - 42, hero.y + 14, 32, 14)
        val gap = 8
        val cardW = ((width - 16 - gap * 2) / 3).coerceAtLeast(100)
        options.forEachIndexed { index, option ->
            option.rect = UiRect(x + 8 + index * (cardW + gap), hero.bottom() + 10, cardW, 72)
            option.toggle = UiRect(option.rect.right() - 42, option.rect.y + 12, 32, 14)
        }
        val last = options.last().rect
        filterCard = UiRect(x + 8, last.bottom() + 10, width - 16, 52)
        return filterCard.bottom() + 8 - y
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = MinecraftClient.getInstance().textRenderer
        val enabled = FastItemsConfig.isActive()
        ModPageChrome.hero(
            context, font, hero,
            "Fast Items",
            "Dropped Items als flache Sprites — weniger Last bei vielen Drops.",
            enabled, lastMouseX, lastMouseY, heroToggle
        )
        options.forEach { option ->
            val on = option.getter()
            ModPageChrome.option(
                context, font, option.rect, option.label, option.description,
                selected = on, lastMouseX, lastMouseY, option.toggle, on
            )
        }
        val hovered = filterCard.contains(lastMouseX, lastMouseY)
        UiWidgets.hoverCard(context, filterCard, lastMouseX, lastMouseY, key = "fast-filter")
        context.drawText(font, "Item-Filter", filterCard.x + 12, filterCard.y + 10, HugoTheme.text, false)
        context.drawText(
            font,
            "Nur diese gedroppten Items vereinfachen.",
            filterCard.x + 12,
            filterCard.y + 28,
            HugoTheme.textMuted,
            false
        )
        context.drawText(font, "Öffnen →", filterCard.right() - font.getWidth("Öffnen →") - 12, filterCard.y + 20, HugoTheme.accent, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (heroToggle.contains(mouseX, mouseY) || hero.contains(mouseX, mouseY) && mouseY <= hero.y + 32) {
            FastItemsConfig.enable = !FastItemsConfig.enable
            persist()
            return true
        }
        options.firstOrNull { it.rect.contains(mouseX, mouseY) || it.toggle.contains(mouseX, mouseY) }?.let { option ->
            option.onChange(!option.getter())
            persist()
            return true
        }
        if (filterCard.contains(mouseX, mouseY)) {
            PopupManager.open(ItemFilterPopup("Fast Items", FastItemsConfig.filter) { persist() })
            return true
        }
        return false
    }

    override fun persist() {
        FastItemsConfig.save()
    }
}
