package dev.henny.hugoutils.ui

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object UiDebugCommands {
    private var initialized = false
    private var runtimeInitialized = false
    private var pendingAction: (() -> Unit)? = null
    private var pendingTicks = 0
    private var lastHudFrameNanos = System.nanoTime()

    /**
     * Registers the release-safe UI commands only when [enabled] is true.
     * Development environments should pass true; release callers supply their
     * persistent debug setting.
     */
    @Synchronized
    fun initialize(enabled: Boolean, openPage: (String) -> Boolean = { false }) {
        initializeRuntime()
        if (!enabled || initialized) return
        initialized = true
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            val root = literal("hugoutils-ui")
                .then(
                    literal("demo").apply {
                        UiDemo.entries.forEach { demo ->
                            then(literal(demo.id).executes {
                                afterChatCloses {
                                    MinecraftClient.getInstance().setScreen(UiDemoScreen(demo))
                                }
                                1
                            })
                        }
                    }
                )
                .then(
                    literal("open").then(
                        argument("pageId", StringArgumentType.word()).executes { context ->
                            val pageId = StringArgumentType.getString(context, "pageId")
                            afterChatCloses {
                                if (!openPage(pageId)) {
                                    MinecraftClient.getInstance().player?.sendMessage(
                                        Text.literal("Unbekannte HugoUtils-Seite: $pageId"),
                                        false
                                    )
                                }
                            }
                            1
                        }
                    )
                )
                .then(
                    literal("toast").then(
                        argument("text", StringArgumentType.greedyString()).executes { context ->
                            UiOverlays.host.show(Toast(StringArgumentType.getString(context, "text")))
                            context.source.sendFeedback(Text.literal("Toast wurde vorgemerkt."))
                            1
                        }
                    )
                )
            dispatcher.register(root)
        }
    }

    @Synchronized
    private fun initializeRuntime() {
        if (runtimeInitialized) return
        runtimeInitialized = true

        ClientTickEvents.END_CLIENT_TICK.register {
            val action = pendingAction ?: return@register
            if (pendingTicks-- <= 0) {
                pendingAction = null
                action()
            }
        }

        HudElementRegistry.addLast(Identifier.of("hugoutils-ui", "toasts")) { context, _ ->
            val now = System.nanoTime()
            val deltaSeconds = ((now - lastHudFrameNanos) / 1_000_000_000.0).toFloat().coerceIn(0f, .1f)
            lastHudFrameNanos = now
            UiOverlays.host.update(deltaSeconds)
            val client = MinecraftClient.getInstance()
            UiOverlays.host.renderToasts(
                context,
                client.textRenderer,
                client.window.scaledWidth,
                client.window.scaledHeight
            )
        }
    }

    @Synchronized
    private fun afterChatCloses(action: () -> Unit) {
        pendingAction = action
        pendingTicks = 1
    }
}
