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
    private val entrance = AnimatedFloat(0f, .22f, Easing.EASE_OUT)
    protected val pageTransition = AnimatedFloat(1f, .18f, Easing.EASE_OUT)

    override fun shouldPause(): Boolean = false

    override fun init() {
        super.init()
        entrance.snapTo(0f)
        entrance.animateTo(1f)
        if (selectedPageId == null) {
            selectedPageId = navigation.roots().firstNotNullOfOrNull { root ->
                navigation.children(root.id).firstOrNull()?.id ?: root.id
            }
        }
        relayoutShell()
    }

    protected open fun relayoutShell() {
        val margin = if (height < 280) 8 else 14
        val panelWidth = (width - margin * 2).coerceIn(UiMetrics.PANEL_MIN_WIDTH, UiMetrics.PANEL_MAX_WIDTH)
        val panelHeight = (height - margin * 2).coerceIn(UiMetrics.PANEL_MIN_HEIGHT, UiMetrics.PANEL_MAX_HEIGHT)
        panel = UiRect((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight)
        val sidebarWidth = (panelWidth * .27f).toInt().coerceIn(108, 142)
        sidebar = UiRect(panel.x, panel.y, sidebarWidth, panelHeight)
        content = UiRect(panel.x + sidebarWidth + 14, panel.y + 12, panel.w - sidebarWidth - 26, panel.h - 24)
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        context.fill(0, 0, width, height, UiDraw.theme.overlay)
    }

    protected fun renderShell(context: DrawContext) {
        UiFrame.beginFrame()
        entrance.update(UiFrame.deltaSeconds)
        pageTransition.update(UiFrame.deltaSeconds)
        val animatedPanel = panel.scaleFromCenter(.975f + .025f * entrance.value)
        UiDraw.shadow(context, animatedPanel, entrance.value)
        UiDraw.panel(context, animatedPanel, UiDraw.alpha(UiDraw.theme.panel, entrance.value), UiDraw.alpha(UiDraw.theme.panelBorder, entrance.value))
        UiDraw.fill(context, sidebar, UiDraw.alpha(UiDraw.theme.sidebar, entrance.value))
    }

    protected fun animatePageChange() {
        pageTransition.snapTo(0f)
        pageTransition.animateTo(1f)
    }
}
