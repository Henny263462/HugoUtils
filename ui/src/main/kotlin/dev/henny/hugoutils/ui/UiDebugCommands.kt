package dev.henny.hugoutils.ui

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object UiDebugCommands {
    private var initialized = false

    /**
     * Registers the release-safe UI commands only when [enabled] is true.
     * Development environments should pass true; release callers supply their
     * persistent debug setting.
     */
    @Synchronized
    fun initialize(enabled: Boolean, openPage: (String) -> Boolean = { false }) {
        if (!enabled || initialized) return
        initialized = true
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            val root = literal("hugoutils-ui")
                .then(
                    literal("demo").apply {
                        UiDemo.entries.forEach { demo ->
                            then(literal(demo.id).executes {
                                MinecraftClient.getInstance().setScreen(UiDemoScreen(demo))
                                1
                            })
                        }
                    }
                )
                .then(
                    literal("open").then(
                        argument("pageId", StringArgumentType.word()).executes { context ->
                            val pageId = StringArgumentType.getString(context, "pageId")
                            if (openPage(pageId)) 1 else {
                                context.source.sendError(Text.literal("Unbekannte HugoUtils-Seite: $pageId"))
                                0
                            }
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
}
