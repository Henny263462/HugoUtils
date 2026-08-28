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
    private val helperSpawned = AtomicBoolean(false)
    private val installOnExitRequested = AtomicBoolean(false)

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
            state == UpdateState.READY_TO_INSTALL ||
            state == UpdateState.DOWNLOADING ||
            state == UpdateState.VERIFYING
    }

    fun initialize() {
        val installed = installedVersion()
        status = UpdateStatus(state = UpdateState.UNKNOWN, installedVersion = installed)
        ClientLifecycleEvents.CLIENT_STARTED.register {
            if (ConfigManager.config.checkUpdatesAutomatically) {
                checkForUpdates()
            }
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            if (installOnExitRequested.get()) {
                spawnHelper(restart = false)
            }
        }
        ClientTickEvents.END_CLIENT_TICK.register { client -> maybeNotify(client) }
    }

    fun checkForUpdates() {
        if (status.state == UpdateState.DOWNLOADING ||
            status.state == UpdateState.VERIFYING ||
            status.state == UpdateState.INSTALLING
        ) {
            return
        }
        if (!busy.compareAndSet(false, true)) return
        status = status.copy(state = UpdateState.CHECKING, message = "Checking for updates…")
        executor.execute {
            try {
                performCheck()
            } catch (error: Exception) {
                logger.warn("Update check failed: {}", error.message)
                status = status.copy(state = UpdateState.ERROR, message = userMessage(error))
            } finally {
                busy.set(false)
            }
        }
    }

    fun downloadUpdate() {
        val current = pending
        if (current == null || status.state != UpdateState.UPDATE_AVAILABLE) return
        if (!busy.compareAndSet(false, true)) return
        status = status.copy(state = UpdateState.DOWNLOADING, progress = 0f, message = "Downloading update…")
        executor.execute {
            try {
                performDownload(current)
            } catch (error: Exception) {
                logger.warn("Update download failed: {}", error.message)
                pending = current.copy(stagedJar = null, sha256 = null)
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

    fun installOnExit(): Boolean {
        val ready = pending
        if (ready?.stagedJar == null || ready.sha256 == null || status.state != UpdateState.READY_TO_INSTALL) {
            return false
        }
        installOnExitRequested.set(true)
        spawnHelper(restart = false)
        status = status.copy(message = "Update will be installed when Minecraft exits.")
        return true
    }

    fun restartAndUpdate(): Boolean {
        val ready = pending
        if (ready?.stagedJar == null || ready.sha256 == null || status.state != UpdateState.READY_TO_INSTALL) {
            return false
        }
        val launched = spawnHelper(restart = true)
        val client = MinecraftClient.getInstance()
        client.execute { client.scheduleStop() }
        if (!launched || !status.restartSupported) {
            status = status.copy(
                state = UpdateState.INSTALLING,
                message = "Update will be installed on exit. Relaunch Minecraft afterwards."
            )
        } else {
            status = status.copy(state = UpdateState.INSTALLING, message = "Restarting to install the update…")
        }
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
        pending = PendingUpdate(
            release = latest,
            asset = asset,
            currentJar = currentJar,
            destination = destinationPath(currentJar, latest.version?.original ?: latest.tagName)
        )
        status = UpdateStatus(
            state = UpdateState.UPDATE_AVAILABLE,
            installedVersion = installedRaw,
            availableVersion = displayVersion(latest),
            message = "Update available: ${displayVersion(latest)}",
            restartSupported = currentJar != null && restartCommand() != null
        )
    }

    private fun performDownload(update: PendingUpdate) {
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
        status = status.copy(state = UpdateState.VERIFYING, progress = 0.8f, message = "Verifying checksum…")
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
        val currentJar = update.currentJar ?: currentJarPath()
            ?: throw UpdateFailure("The loaded HugoUtils JAR could not be located.")
        pending = update.copy(
            stagedJar = stagedJar,
            sha256 = expected,
            currentJar = currentJar,
            destination = destinationPath(currentJar, update.release.version?.original ?: update.release.tagName)
        )
        status = status.copy(
            state = UpdateState.READY_TO_INSTALL,
            availableVersion = displayVersion(update.release),
            message = "Ready to install ${displayVersion(update.release)}",
            progress = 1f,
            restartSupported = restartCommand() != null
        )
    }

    private fun spawnHelper(restart: Boolean): Boolean {
        val update = pending
        val staged = update?.stagedJar
        val hash = update?.sha256
        val current = update?.currentJar
        val destination = update?.destination
        if (update == null || staged == null || hash == null || current == null || destination == null) {
            return false
        }
        if (!helperSpawned.compareAndSet(false, true)) {
            return true
        }
        return try {
            val helperJar = materializeHelper()
            val java = ProcessHandle.current().info().command().orElse("java")
            val command = mutableListOf(
                java,
                "-cp",
                helperJar.toAbsolutePath().toString(),
                "dev.henny.hugoutils.update.UpdateHelper",
                "--pid",
                ProcessHandle.current().pid().toString(),
                "--target",
                current.toAbsolutePath().toString(),
                "--staged",
                staged.toAbsolutePath().toString(),
                "--destination",
                destination.toAbsolutePath().toString(),
                "--sha256",
                hash
            )
            val restartLaunch = if (restart) restartCommand() else null
            if (restartLaunch != null) {
                command += "--restart"
            }
            val log = stagingDir().resolve("update-helper.log")
            val process = startHelperProcess(command, log, restartLaunch != null)
            if (restartLaunch != null) {
                process.outputStream.bufferedWriter().use { writer ->
                    writer.append("workdir=").append(restartLaunch.workDir.toAbsolutePath().toString()).append('\n')
                    writer.append("command=").append(restartLaunch.command).append('\n')
                    restartLaunch.arguments.forEach { arg ->
                        writer.append("arg=").append(arg).append('\n')
                    }
                    writer.append("END\n")
                }
            }
            true
        } catch (error: Exception) {
            helperSpawned.set(false)
            logger.warn("Could not start the update helper: {}", error.message)
            status = status.copy(state = UpdateState.ERROR, message = "The update helper could not be started.")
            false
        }
    }

    private fun startHelperProcess(command: List<String>, log: Path, keepStdin: Boolean): Process {
        val windows = System.getProperty("os.name").lowercase().contains("win")
        val launched = if (windows && !keepStdin) {
            mutableListOf("cmd.exe", "/c", "start", "", "/b") + command
        } else {
            command
        }
        val builder = ProcessBuilder(launched)
        builder.directory(stagingDir().toFile())
        builder.redirectOutput(log.toFile())
        builder.redirectError(log.toFile())
        return builder.start()
    }

    private fun materializeHelper(): Path {
        val staging = stagingDir()
        Files.createDirectories(staging)
        val helper = staging.resolve("update-helper.jar")
        val source = helperSource()
        Files.copy(source, helper, StandardCopyOption.REPLACE_EXISTING)
        return helper
    }

    private fun helperSource(): Path {
        val container = FabricLoader.getInstance().getModContainer("hugoutils-core").orElse(null)
            ?: throw UpdateFailure("HugoUtils core could not be located.")
        val origin = container.origin.paths.firstOrNull { Files.exists(it) }
            ?: throw UpdateFailure("HugoUtils core could not be located.")
        if (Files.isRegularFile(origin)) return origin
        val nested = FabricLoader.getInstance().getModContainer(HugoIds.MOD_ID).orElse(null)
            ?.origin?.paths?.firstOrNull { Files.isRegularFile(it) && it.fileName.toString().endsWith(".jar") }
        if (nested != null) return nested
        throw UpdateFailure("The update helper could not be copied.")
    }

    private fun restartCommand(): RestartLaunch? {
        val info = ProcessHandle.current().info()
        val command = info.command().orElse(null)?.takeIf { it.isNotBlank() } ?: return null
        val arguments = info.arguments().orElse(null)?.toList() ?: return null
        val workDir = FabricLoader.getInstance().gameDir
        return RestartLaunch(workDir, command, arguments)
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

    private fun destinationPath(currentJar: Path?, version: String): Path? {
        val parent = currentJar?.parent ?: return null
        val cleaned = version.removePrefix("v").removePrefix("V")
        return parent.resolve("HugoUtils-$cleaned.jar")
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
        if (snapshot.state != UpdateState.UPDATE_AVAILABLE && snapshot.state != UpdateState.READY_TO_INSTALL) {
            return
        }
        if (notifiedTag == tag || client.player == null) return
        notifiedTag = tag
        client.player?.sendMessage(Text.literal("HugoUtils: Update available ($tag)."), true)
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

    private data class RestartLaunch(
        val workDir: Path,
        val command: String,
        val arguments: List<String>
    )

    private class UpdateFailure(message: String) : RuntimeException(message)
}
