package dev.henny.hugoutils.api

import net.minecraft.client.MinecraftClient
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ClientJobs {
    private val executor = ThreadPoolExecutor(
        4,
        4,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUED_JOBS),
        { runnable -> Thread(runnable, "HugoUtils-ClientApi").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy()
    )

    fun submit(onStatus: (String, Boolean) -> Unit, block: () -> String) {
        try {
            executor.execute {
                try {
                    val message = block()
                    MinecraftClient.getInstance().execute { onStatus(message, false) }
                } catch (error: ClientApiException) {
                    MinecraftClient.getInstance().execute {
                        onStatus(ClientAuth.userMessage(error.error, error.message), true)
                    }
                } catch (_: Exception) {
                    MinecraftClient.getInstance().execute { onStatus("API ist gerade nicht erreichbar.", true) }
                }
            }
        } catch (_: RejectedExecutionException) {
            MinecraftClient.getInstance().execute {
                onStatus("Zu viele API-Anfragen. Bitte kurz warten.", true)
            }
        }
    }

    private const val MAX_QUEUED_JOBS = 16
}
