package dev.henny.hugoutils.sky

import com.mojang.brigadier.arguments.IntegerArgumentType
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.ui.ConfigPages
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.text.Text

class SkyMod : ClientModInitializer {
    override fun onInitializeClient() {
        ConfigManager.registerSection(SkyManager)
        ConfigPages.register(SkyConfigPage())
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("sky")
                    .executes { context ->
                        context.source.sendFeedback(Text.literal(SkyManager.describe()))
                        1
                    }
                    .then(
                        literal("scan").executes { context ->
                            SkyManager.scan {
                                context.source.sendFeedback(Text.literal(SkyManager.describe()))
                            }
                            context.source.sendFeedback(Text.literal("Sky-Texturepacks werden asynchron gesucht …"))
                            1
                        }
                    )
                    .then(
                        argument("index", IntegerArgumentType.integer(1)).executes { context ->
                            val index = IntegerArgumentType.getInteger(context, "index")
                            SkyManager.select(index) { message, error ->
                                if (error) context.source.sendError(Text.literal(message))
                                else context.source.sendFeedback(Text.literal(message))
                            }
                            1
                        }
                    )
            )
        }
        SkyManager.scan()
    }
}
