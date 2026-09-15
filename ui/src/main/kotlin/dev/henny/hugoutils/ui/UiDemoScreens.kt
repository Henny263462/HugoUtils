package dev.henny.hugoutils.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

enum class UiDemo(val id: String, val title: String) {
    GALLERY("gallery", "Widget Gallery"),
    FORM("form", "Formular"),
    LIST("list", "Suchbare Liste"),
    DIALOG("dialog", "Dialog & Toast"),
    NAVIGATION("navigation", "Navigation");

    companion object {
        fun fromId(id: String): UiDemo? = entries.firstOrNull { it.id == id }
    }
}

class UiDemoScreen(private val demo: UiDemo) : ScreenShell(Text.literal("HugoUtils UI · ${demo.title}")) {
    private val client = MinecraftClient.getInstance()
    private val demoRegistry = NavigationRegistry().apply {
        register(NavigationEntry("components", "Komponenten", order = 0))
        register(NavigationEntry("forms", "Formulare", parentId = "components", order = 1))
        register(NavigationEntry("lists", "Listen", parentId = "components", order = 2))
        register(NavigationEntry("about", "Über UI", order = 3))
    }
    private val toggle = Toggle(initialValue = true, onChange = {})
    private val slider = Slider(.65f)
    private val field = TextField(placeholder = "Text eingeben …")
    private val search = SearchList(
        listOf("minecraft:flame", "minecraft:smoke", "minecraft:portal", "minecraft:happy_villager")
    )
    private var toastButton = UiRect(0, 0, 0, 0)
    private var dialogButton = UiRect(0, 0, 0, 0)

    init {
        if (demo == UiDemo.NAVIGATION) {
            expandedParents += "components"
            selectedPageId = "forms"
        }
    }

    override fun init() {
        super.init()
        field.logic.setText(if (demo == UiDemo.LIST) "portal" else "")
        search.query = field.logic.text
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderBackground(context, mouseX, mouseY, deltaTicks)
        relayoutShell()
        renderShell(context)
        val x = content.x + 8
        var y = content.y + 8
        context.drawText(textRenderer, demo.title, x, y, UiDraw.theme.text, false)
        y += 22

        when (demo) {
            UiDemo.GALLERY -> renderGallery(context, mouseX, mouseY, x, y)
            UiDemo.FORM -> renderForm(context, mouseX, mouseY, x, y)
            UiDemo.LIST -> renderList(context, mouseX, mouseY, x, y)
            UiDemo.DIALOG -> renderDialogDemo(context, mouseX, mouseY, x, y)
            UiDemo.NAVIGATION -> renderNavigation(context, x, y)
        }

        UiOverlays.host.active?.let {
            it.layout(width, height)
            it.render(context, mouseX, mouseY)
        }
        UiOverlays.host.update((deltaTicks / 20f).coerceIn(0f, .1f))
        UiOverlays.host.renderToasts(context, textRenderer, width, height)
        super.render(context, mouseX, mouseY, deltaTicks)
    }

    private fun renderGallery(context: DrawContext, mouseX: Int, mouseY: Int, x: Int, y: Int) {
        val card = UiRect(x, y, content.w - 16, 120)
        UiDraw.panel(context, card, UiDraw.theme.card, UiDraw.theme.border)
        toggle.bounds = UiRect(x + 12, y + 16, 34, 14)
        toggle.tick(.05f)
        toggle.render(context, textRenderer, mouseX, mouseY)
        slider.bounds = UiRect(x + 12, y + 50, card.w - 24, 12)
        slider.render(context, textRenderer, mouseX, mouseY)
        field.bounds = UiRect(x + 12, y + 78, card.w - 24, 20)
        field.render(context, textRenderer, mouseX, mouseY)
    }

    private fun renderForm(context: DrawContext, mouseX: Int, mouseY: Int, x: Int, y: Int) {
        context.drawText(textRenderer, "Öffentliche Controls ohne Core-Abhängigkeit", x, y, UiDraw.theme.textMuted, false)
        field.bounds = UiRect(x, y + 22, content.w - 16, 20)
        field.render(context, textRenderer, mouseX, mouseY)
        toggle.bounds = UiRect(x, y + 56, 34, 14)
        toggle.tick(.05f)
        toggle.render(context, textRenderer, mouseX, mouseY)
    }

    private fun renderList(context: DrawContext, mouseX: Int, mouseY: Int, x: Int, y: Int) {
        field.bounds = UiRect(x, y, content.w - 16, 20)
        field.render(context, textRenderer, mouseX, mouseY)
        search.query = field.logic.text
        search.filtered.forEachIndexed { index, value ->
            val row = UiRect(x, y + 28 + index * 22, content.w - 16, 20)
            UiDraw.panel(context, row, UiDraw.theme.inset, UiDraw.theme.border)
            context.drawText(textRenderer, value, row.x + 6, row.y + 6, UiDraw.theme.text, false)
        }
    }

    private fun renderDialogDemo(context: DrawContext, mouseX: Int, mouseY: Int, x: Int, y: Int) {
        toastButton = UiRect(x, y, 120, 22)
        dialogButton = UiRect(x + 128, y, 120, 22)
        Button("Toast anzeigen", {
            UiOverlays.host.show(Toast("UI-Toast funktioniert", kind = Toast.Kind.SUCCESS))
        }, toastButton).render(context, textRenderer, mouseX, mouseY)
        Button("Dialog öffnen", {
            UiOverlays.host.open(Dialog("Beispiel", "Öffentlicher Dialog-Host"))
        }, dialogButton).render(context, textRenderer, mouseX, mouseY)
    }

    private fun renderNavigation(context: DrawContext, x: Int, y: Int) {
        context.drawText(textRenderer, "Verschachtelte Navigation", x, y, UiDraw.theme.textMuted, false)
        var rowY = y + 20
        demoRegistry.roots().forEach { root ->
            context.drawText(textRenderer, "▾ ${root.title}", x, rowY, UiDraw.theme.accent, false)
            rowY += 16
            demoRegistry.children(root.id).forEach { child ->
                context.drawText(textRenderer, "  • ${child.title}", x + 8, rowY, UiDraw.theme.text, false)
                rowY += 16
            }
        }
    }

    override fun mouseClicked(click: net.minecraft.client.gui.Click, doubled: Boolean): Boolean {
        val x = click.x()
        val y = click.y()
        UiOverlays.host.active?.let { return it.mouseClicked(x, y) }
        if (demo == UiDemo.DIALOG) {
            if (toastButton.contains(x, y)) {
                UiOverlays.host.show(Toast("UI-Toast funktioniert", kind = Toast.Kind.SUCCESS))
                return true
            }
            if (dialogButton.contains(x, y)) {
                UiOverlays.host.open(Dialog("Beispiel", "Öffentlicher Dialog-Host"))
                return true
            }
        }
        return field.mouseClicked(x, y) || toggle.mouseClicked(x, y) || slider.mouseClicked(x, y) ||
            super.mouseClicked(click, doubled)
    }

    override fun close() {
        UiOverlays.host.closeAll()
        client.setScreen(null)
    }
}
