package dev.henny.hugoutils.update

import dev.henny.hugoutils.HugoIds
import dev.henny.hugoutils.api.ApiConfig
import dev.henny.hugoutils.client.config.ConfigManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object UpdateManager {
    private val logger = LoggerFactory.getLogger("HugoUtils/Update")
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "HugoUtils-Update").apply { isDaemon = true }
    }
    private val busy = AtomicBoolean(false)

    @Volatile
    private var status: UpdateStatus = UpdateStatus()

    @Volatile
    private var pending: PendingUpdate? = null

    @Volatile
    private var notifiedTag: String? = null

    fun status(): UpdateStatus = status

    fun hasUpdate(): Boolean {
        val state = status.state
        return state == UpdateState.UPDATE_AVAILABLE ||
            state == UpdateState.PENDING_RESTART ||
            state == UpdateState.DOWNLOADING ||
            state == UpdateState.VERIFYING ||
            state == UpdateState.INSTALLING
    }

    fun initialize() {
        val installed = installedVersion()
        status = UpdateStatus(state = UpdateState.UNKNOWN, installedVersion = installed)
        cleanupDisabledJars()
        ClientLifecycleEvents.CLIENT_STARTED.register {
            if (ConfigManager.config.checkUpdatesAutomatically) {
                checkForUpdates(autoApply = true)
            }
        }
        ClientTickEvents.END_CLIENT_TICK.register { client -> maybeNotify(client) }
    }

    fun checkForUpdates(autoApply: Boolean = false) {
        if (status.state == UpdateState.DOWNLOADING ||
            status.state == UpdateState.VERIFYING ||
            status.state == UpdateState.INSTALLING
        ) {
            return
        }
        if (status.state == UpdateState.PENDING_RESTART) {
            return
        }
        if (!busy.compareAndSet(false, true)) return
        status = status.copy(state = UpdateState.CHECKING, message = "Checking for updates…")
        executor.execute {
            try {
                performCheck()
                if (autoApply && status.state == UpdateState.UPDATE_AVAILABLE) {
                    val update = pending
                    if (update != null) {
                        try {
                            performDownloadAndInstall(update)
                        } catch (error: Exception) {
                            logger.warn("Auto update install failed: {}", error.message)
                            status = status.copy(
                                state = UpdateState.UPDATE_AVAILABLE,
                                message = userMessage(error),
                                progress = 0f
                            )
                        }
                    }
                }
            } catch (error: Exception) {
                logger.warn("Update check failed: {}", error.message)
                status = status.copy(state = UpdateState.ERROR, message = userMessage(error))
            } finally {
                busy.set(false)
            }
        }
    }

    fun downloadAndInstall() {
        val current = pending
        if (current == null || status.state != UpdateState.UPDATE_AVAILABLE) return
        if (!busy.compareAndSet(false, true)) return
        status = status.copy(state = UpdateState.DOWNLOADING, progress = 0f, message = "Downloading update…")
        executor.execute {
            try {
                performDownloadAndInstall(current)
            } catch (error: Exception) {
                logger.warn("Update install failed: {}", error.message)
                status = status.copy(
                    state = UpdateState.UPDATE_AVAILABLE,
                    message = userMessage(error),
                    progress = 0f
                )
            } finally {
                busy.set(false)
            }
        }
    }

    /** @deprecated Use [downloadAndInstall]. */
    fun downloadUpdate() = downloadAndInstall()

    fun quitToApply(): Boolean {
        if (status.state != UpdateState.PENDING_RESTART) return false
        val client = MinecraftClient.getInstance()
        client.execute { client.scheduleStop() }
        status = status.copy(message = "Closing Minecraft to load the new HugoUtils version…")
        return true
    }

    private fun performCheck() {
        val installedRaw = installedVersion() ?: "0.0.0"
        val installed = SemVer.parse(installedRaw) ?: SemVer.parse("0.0.0")!!
        val request = HttpRequest.newBuilder(URI.create(ApiConfig.GITHUB_RELEASES_URL + "?per_page=30"))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", ApiConfig.USER_AGENT)
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        when (response.statusCode()) {
            200 -> Unit
            403, 429 -> throw UpdateFailure("GitHub rate limit reached. Try again later.")
            else -> throw UpdateFailure("GitHub is unavailable (HTTP ${response.statusCode()}).")
        }
        val releases = try {
            GitHubReleaseParser.parseList(response.body())
        } catch (_: Exception) {
            throw UpdateFailure("GitHub returned malformed release data.")
        }
        val latest = ReleaseSelector.latest(releases, installed, ConfigManager.config.includePrereleases)
        if (latest == null) {
            pending = null
            status = UpdateStatus(
                state = UpdateState.UP_TO_DATE,
                installedVersion = installedRaw,
                message = "HugoUtils is up to date"
            )
            return
        }
        val asset = ReleaseSelector.selectJar(latest)
            ?: throw UpdateFailure("The latest release has no installable HugoUtils JAR.")
        val currentJar = currentJarPath()
        val version = latest.version?.original ?: latest.tagName
        pending = PendingUpdate(
            release = latest,
            asset = asset,
            currentJar = currentJar,
            destination = destinationPath(version)
        )
        status = UpdateStatus(
            state = UpdateState.UPDATE_AVAILABLE,
            installedVersion = installedRaw,
            availableVersion = displayVersion(latest),
            message = "Update available: ${displayVersion(latest)}",
            restartSupported = true
        )
    }

    private fun performDownloadAndInstall(update: PendingUpdate) {
        val staging = stagingDir()
        Files.createDirectories(staging)
        val jarName = update.asset.name
        val tempJar = staging.resolve("$jarName.part")
        val stagedJar = staging.resolve(jarName)
        Files.deleteIfExists(tempJar)
        Files.deleteIfExists(stagedJar)

        status = status.copy(state = UpdateState.DOWNLOADING, progress = 0.1f, message = "Downloading update…")
        download(update.asset.browserDownloadUrl, tempJar)
        try {
            Files.move(tempJar, stagedJar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(tempJar, stagedJar, StandardCopyOption.REPLACE_EXISTING)
        }

        status = status.copy(state = UpdateState.VERIFYING, progress = 0.7f, message = "Verifying checksum…")
        val sumsBody = downloadText(checksumUrl(update.release, jarName))
            ?: throw UpdateFailure("The latest release is missing SHA256SUMS.txt.")
        val sums = Checksums.parse(sumsBody)
        val expected = Checksums.hashFor(sums, jarName)
            ?: throw UpdateFailure("SHA256SUMS.txt has no checksum for $jarName.")
        val actual = Checksums.sha256(stagedJar)
        if (!Checksums.matches(actual, expected)) {
            Files.deleteIfExists(stagedJar)
            throw UpdateFailure("The downloaded update failed SHA-256 verification.")
        }

        status = status.copy(state = UpdateState.INSTALLING, progress = 0.9f, message = "Installing update…")
        val destination = update.destination
            ?: destinationPath(update.release.version?.original ?: update.release.tagName)
            ?: throw UpdateFailure("Could not resolve the mods folder.")
        val currentJar = update.currentJar ?: currentJarPath()
        val result = try {
            DropInInstaller.install(stagedJar, destination, expected, currentJar)
        } catch (error: Exception) {
            throw UpdateFailure(error.message ?: "Could not install the update into the mods folder.")
        }

        pending = update.copy(
            stagedJar = stagedJar,
            sha256 = expected,
            currentJar = currentJar,
            destination = result.installed
        )

        val versionLabel = displayVersion(update.release)
        val message = if (result.failedToDisable.isEmpty()) {
            "Update $versionLabel installed. Restart Minecraft to finish."
        } else {
            "Update $versionLabel downloaded, but the old JAR could not be renamed. " +
                "Delete the old HugoUtils JAR in mods/ after closing Minecraft, then restart."
        }
        status = UpdateStatus(
            state = UpdateState.PENDING_RESTART,
            installedVersion = installedVersion(),
            availableVersion = versionLabel,
            message = message,
            progress = 1f,
            restartSupported = true
        )
        logger.info(
            "HugoUtils update ready at {} (disabled {} old jar(s), {} failed)",
            result.installed.fileName,
            result.disabled.size,
            result.failedToDisable.size
        )
    }

    private fun cleanupDisabledJars() {
        val mods = modsDir()
        if (!Files.isDirectory(mods)) return
        try {
            Files.list(mods).use { stream ->
                stream.filter { Files.isRegularFile(it) }.forEach { path ->
                    val name = path.fileName.toString().lowercase()
                    if (name.endsWith(".jar.disabled") || name.endsWith(".jar.old")) {
                        try {
                            Files.deleteIfExists(path)
                        } catch (_: Exception) {
                            // Still locked or in use — ignore.
                        }
                    }
                }
            }
        } catch (error: Exception) {
            logger.debug("Could not clean disabled update jars: {}", error.message)
        }
    }

    private fun download(url: String, destination: Path) {
        if (!isTrustedDownload(url)) {
            throw UpdateFailure("Refusing to download from an untrusted host.")
        }
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMinutes(2))
            .header("User-Agent", ApiConfig.USER_AGENT)
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofFile(destination))
        if (response.statusCode() != 200) {
            Files.deleteIfExists(destination)
            throw UpdateFailure("The update could not be downloaded (HTTP ${response.statusCode()}).")
        }
    }

    private fun isTrustedDownload(url: String): Boolean {
        val ownerRepo = "${ApiConfig.GITHUB_OWNER}/${ApiConfig.GITHUB_REPO}"
        return url.startsWith("https://github.com/$ownerRepo/") ||
            url.startsWith("https://objects.githubusercontent.com/") ||
            url.startsWith("https://release-assets.githubusercontent.com/")
    }

    private fun downloadText(url: String): String? {
        if (!isTrustedDownload(url) && !url.startsWith("https://github.com/")) return null
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("User-Agent", ApiConfig.USER_AGENT)
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return if (response.statusCode() == 200) response.body() else null
    }

    private fun checksumUrl(release: GitHubRelease, jarName: String): String {
        val sums = release.assets.firstOrNull { it.name.equals("SHA256SUMS.txt", ignoreCase = true) }
        if (sums != null) return sums.browserDownloadUrl
        val tag = release.tagName
        return "https://github.com/${ApiConfig.GITHUB_OWNER}/${ApiConfig.GITHUB_REPO}/releases/download/$tag/SHA256SUMS.txt"
    }

    private fun currentJarPath(): Path? {
        val container = FabricLoader.getInstance().getModContainer(HugoIds.MOD_ID).orElse(null) ?: return null
        return container.origin.paths.firstOrNull { path ->
            Files.isRegularFile(path) && path.fileName.toString().endsWith(".jar", ignoreCase = true)
        }
    }

    private fun modsDir(): Path {
        currentJarPath()?.parent?.let { return it }
        return FabricLoader.getInstance().gameDir.resolve("mods")
    }

    private fun destinationPath(version: String): Path {
        val cleaned = version.removePrefix("v").removePrefix("V")
        return modsDir().resolve("HugoUtils-$cleaned.jar")
    }

    private fun stagingDir(): Path =
        FabricLoader.getInstance().gameDir.resolve(".hugoutils-update")

    private fun installedVersion(): String? =
        FabricLoader.getInstance().getModContainer(HugoIds.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse(null)

    private fun displayVersion(release: GitHubRelease): String {
        val parsed = release.version?.original ?: release.tagName.removePrefix("v").removePrefix("V")
        return "v$parsed"
    }

    private fun maybeNotify(client: MinecraftClient) {
        val snapshot = status
        val tag = snapshot.availableVersion ?: return
        if (snapshot.state != UpdateState.UPDATE_AVAILABLE && snapshot.state != UpdateState.PENDING_RESTART) {
            return
        }
        if (notifiedTag == tag || client.player == null) return
        notifiedTag = tag
        val text = if (snapshot.state == UpdateState.PENDING_RESTART) {
            "HugoUtils: Update $tag ready — restart Minecraft."
        } else {
            "HugoUtils: Update available ($tag)."
        }
        client.player?.sendMessage(Text.literal(text), true)
    }

    private fun userMessage(error: Exception): String {
        val text = error.message ?: "Update failed."
        return when {
            error is java.net.ConnectException || error is java.net.UnknownHostException ||
                error is java.net.http.HttpTimeoutException ||
                error is java.net.http.HttpConnectTimeoutException ->
                "GitHub is unreachable. You can keep using HugoUtils offline."
            else -> text
        }
    }

    private data class PendingUpdate(
        val release: GitHubRelease,
        val asset: GitHubAsset,
        val currentJar: Path?,
        val destination: Path?,
        val stagedJar: Path? = null,
        val sha256: String? = null
    )

    private class UpdateFailure(message: String) : RuntimeException(message)
}
