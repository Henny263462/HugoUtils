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
    protected var subnav = UiRect(0, 0, 0, 0)
    protected var content = UiRect(0, 0, 0, 0)
    protected val expandedParents = mutableSetOf<String>()
    protected val entrance = AnimatedFloat(0f, .36f, Easing.EASE_OUT)
    protected val pageTransition = AnimatedFloat(1f, .26f, Easing.EASE_IN_OUT)
    protected val subnavReveal = AnimatedFloat(0f, .22f, Easing.EASE_OUT)
    private var frameTicked = false

    override fun shouldPause(): Boolean = false

    override fun init() {
        super.init()
        entrance.snapTo(0f)
        entrance.animateTo(1f)
        if (selectedPageId == null) {
            selectedPageId = navigation.roots().firstOrNull()?.id
        }
        relayoutShell()
    }

    protected open fun subnavParentId(): String? = null

    protected fun tickShell() {
        if (frameTicked) return
        frameTicked = true
        UiFrame.beginFrame()
        entrance.update(UiFrame.deltaSeconds)
        pageTransition.update(UiFrame.deltaSeconds)
        subnavReveal.animateTo(if (subnavParentId() != null) 1f else 0f)
        subnavReveal.update(UiFrame.deltaSeconds)
    }

    protected open fun relayoutShell() {
        val margin = if (height < 280) 6 else 8
        val panelWidth = (width - margin * 2).coerceIn(UiMetrics.PANEL_MIN_WIDTH, UiMetrics.PANEL_MAX_WIDTH)
        val panelHeight = (height - margin * 2).coerceIn(UiMetrics.PANEL_MIN_HEIGHT, UiMetrics.PANEL_MAX_HEIGHT)
        val rise = ((1f - entrance.value) * 22f).toInt()
        panel = UiRect((width - panelWidth) / 2, (height - panelHeight) / 2 + rise, panelWidth, panelHeight)
        val railWidth = UiMetrics.RAIL_WIDTH.coerceAtMost(panelWidth / 3)
        sidebar = UiRect(panel.x, panel.y, railWidth, panelHeight)
        val subWidth = (UiMetrics.SUBNAV_WIDTH * subnavReveal.value).toInt()
        subnav = UiRect(sidebar.right, panel.y, subWidth, panelHeight)
        val contentX = subnav.right + 10
        content = UiRect(contentX, panel.y + 8, (panel.right - contentX - 10).coerceAtLeast(120), panel.h - 16)
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val fade = 0.28f + 0.72f * entrance.value
        context.fill(0, 0, width, height, UiDraw.alpha(UiDraw.theme.overlay, fade))
    }

    protected fun renderShell(context: DrawContext) {
        tickShell()
        frameTicked = false
        val alpha = entrance.value
        UiDraw.shadow(context, panel, alpha)
        UiDraw.panel(
            context,
            panel,
            UiDraw.alpha(UiDraw.theme.panel, alpha),
            UiDraw.alpha(UiDraw.theme.panelBorder, alpha)
        )
        UiDraw.fill(context, sidebar.x + 1, sidebar.y + 1, sidebar.w - 1, sidebar.h - 2, UiDraw.alpha(UiDraw.theme.sidebar, alpha))
        if (subnav.w > 8) {
            UiDraw.fill(context, subnav.x, subnav.y + 1, subnav.w, subnav.h - 2, UiDraw.alpha(UiDraw.theme.inset, alpha))
        }
    }

    protected fun animatePageChange() {
        pageTransition.snapTo(0f)
        pageTransition.animateTo(1f)
    }
}
