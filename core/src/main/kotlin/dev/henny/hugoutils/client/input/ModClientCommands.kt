package dev.henny.hugoutils.client.input

import com.mojang.brigadier.arguments.StringArgumentType
import dev.henny.hugoutils.client.access.WebLoginClient
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object ModClientCommands {
    fun initialize() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("login")
                    .then(
                        argument("code", StringArgumentType.word())
                            .executes { context ->
                                WebLoginClient.login(
                                    StringArgumentType.getString(context, "code"),
                                    context.source
                                )
                                1
                            }
                    )
            )
        }
    }
}
