package dev.henny.hugoutils.client.input

import dev.henny.hugoutils.api.AuthApiClient
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object ModClientCommands {
    fun initialize() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("login").executes { context ->
                    AuthApiClient.login(context.source)
                    1
                }
            )
        }
    }
}
