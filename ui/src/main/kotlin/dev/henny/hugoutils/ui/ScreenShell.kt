package dev.henny.hugoutils.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

abstract class ScreenShell(
    title: Text,
    protected val navigation: NavigationRegistry = UiNavigation.registry
) : Screen(title) {
    protected var selectedPageId: String? = null
    protected var panel = UiRect(0, 0, 0, 0)
    protected var sidebar = UiRect(0, 0, 0, 0)
    protected var content = UiRect(0, 0, 0, 0)
    protected val expandedParents = mutableSetOf<String>()

    override fun shouldPause(): Boolean = false

    override fun init() {
        super.init()
        if (selectedPageId == null) {
            selectedPageId = navigation.roots().firstNotNullOfOrNull { root ->
                navigation.children(root.id).firstOrNull()?.id ?: root.id
            }
        }
        relayoutShell()
    }

    protected open fun relayoutShell() {
        val margin = if (height < 280) 8 else 14
        val panelWidth = (width - margin * 2).coerceIn(360, 680)
        val panelHeight = (height - margin * 2).coerceIn(240, 460)
        panel = UiRect((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight)
        val sidebarWidth = (panelWidth * .27f).toInt().coerceIn(108, 142)
        sidebar = UiRect(panel.x, panel.y, sidebarWidth, panelHeight)
        content = UiRect(panel.x + sidebarWidth + 14, panel.y + 12, panel.w - sidebarWidth - 26, panel.h - 24)
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        context.fill(0, 0, width, height, UiDraw.theme.overlay)
    }

    protected fun renderShell(context: DrawContext) {
        UiDraw.panel(context, panel, UiDraw.theme.panel, UiDraw.theme.panelBorder)
        UiDraw.fill(context, sidebar, UiDraw.theme.sidebar)
    }
}
