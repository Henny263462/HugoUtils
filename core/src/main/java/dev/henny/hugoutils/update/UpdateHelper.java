package dev.henny.hugoutils.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class UpdateHelper {
    private UpdateHelper() {}

    public static void main(String[] args) {
        int code = 1;
        try {
            code = run(args);
        } catch (Exception error) {
            System.err.println("HugoUtils update helper failed.");
            error.printStackTrace(System.err);
        }
        System.exit(code);
    }

    public static int run(String[] args) throws Exception {
        Arguments parsed = Arguments.parse(args);
        RestartCommand restart = parsed.restart ? RestartCommand.readStdin() : null;
        waitForProcess(parsed.pid);
        install(parsed.target, parsed.staged, parsed.destination, parsed.sha256);
        if (restart != null) {
            restart.launch();
        }
        return 0;
    }

    public static void install(Path currentJar, Path stagedJar, Path destination, String expectedSha256) throws Exception {
        if (!Files.isRegularFile(stagedJar)) {
            throw new IllegalStateException("staged update is missing");
        }
        String actual = sha256(stagedJar);
        if (!matches(actual, expectedSha256)) {
            throw new IllegalStateException("staged update checksum mismatch");
        }
        Path stagingDir = stagedJar.getParent();
        Files.createDirectories(stagingDir);
        Path backup = uniqueBackup(stagingDir, currentJar.getFileName().toString());
        boolean destExisted = Files.exists(destination) && !destination.equals(currentJar);
        Path extraBackup = destExisted ? uniqueBackup(stagingDir, destination.getFileName().toString()) : null;
        try {
            if (destExisted) {
                move(destination, extraBackup);
            }
            if (Files.exists(currentJar)) {
                move(currentJar, backup);
            }
            move(stagedJar, destination);
            String installedHash = sha256(destination);
            if (!matches(installedHash, expectedSha256)) {
                throw new IllegalStateException("installed update checksum mismatch");
            }
            Files.deleteIfExists(backup);
            if (extraBackup != null) {
                Files.deleteIfExists(extraBackup);
            }
            if (!currentJar.equals(destination)) {
                Files.deleteIfExists(currentJar);
            }
            removeSiblingDuplicates(destination, currentJar);
        } catch (Exception error) {
            try {
                Files.deleteIfExists(destination);
            } catch (Exception ignored) {
            }
            if (Files.exists(backup)) {
                try {
                    move(backup, currentJar);
                } catch (Exception ignored) {
                }
            }
            if (extraBackup != null && Files.exists(extraBackup)) {
                try {
                    move(extraBackup, destination);
                } catch (Exception ignored) {
                }
            }
            throw error;
        }
    }

    static Path uniqueBackup(Path directory, String originalName) {
        String base = originalName.endsWith(".jar")
            ? originalName.substring(0, originalName.length() - 4) + ".bak"
            : originalName + ".bak";
        Path candidate = directory.resolve(base);
        int index = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(base + "." + index);
            index += 1;
        }
        return candidate;
    }

    private static void waitForProcess(long pid) throws InterruptedException {
        if (pid <= 0) {
            return;
        }
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        while (handle.isPresent() && handle.get().isAlive()) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.MILLISECONDS.sleep(400);
    }

    private static void move(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void removeSiblingDuplicates(Path destination, Path previous) throws IOException {
        Path parent = destination.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            return;
        }
        try (var paths = Files.list(parent)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                if (path.equals(destination)) {
                    return;
                }
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                boolean isHugo = name.startsWith("hugoutils-") && name.endsWith(".jar");
                if (isHugo && (path.equals(previous)
                    || name.equals(previous.getFileName().toString().toLowerCase(Locale.ROOT)))) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                }
            });
        }
    }

    public static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    static boolean matches(String actual, String expected) {
        return actual.equalsIgnoreCase(expected);
    }

    public static final class Arguments {
        final long pid;
        final Path target;
        final Path staged;
        final Path destination;
        final String sha256;
        final boolean restart;

        Arguments(long pid, Path target, Path staged, Path destination, String sha256, boolean restart) {
            this.pid = pid;
            this.target = target;
            this.staged = staged;
            this.destination = destination;
            this.sha256 = sha256;
            this.restart = restart;
        }

        static Arguments parse(String[] args) {
            long pid = 0;
            Path target = null;
            Path staged = null;
            Path destination = null;
            String sha256 = null;
            boolean restart = false;
            for (int i = 0; i < args.length; i++) {
                String flag = args[i];
                if ("--restart".equals(flag)) {
                    restart = true;
                    continue;
                }
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("missing value for " + flag);
                }
                String value = args[++i];
                switch (flag) {
                    case "--pid" -> pid = Long.parseLong(value);
                    case "--target" -> target = Path.of(value);
                    case "--staged" -> staged = Path.of(value);
                    case "--destination" -> destination = Path.of(value);
                    case "--sha256" -> sha256 = value;
                    default -> throw new IllegalArgumentException("unknown argument");
                }
            }
            if (target == null || staged == null || sha256 == null) {
                throw new IllegalArgumentException("target, staged, and sha256 are required");
            }
            if (destination == null) {
                destination = target;
            }
            return new Arguments(pid, target, staged, destination, sha256, restart);
        }
    }

    static final class RestartCommand {
        final Path workDir;
        final String command;
        final List<String> arguments;

        RestartCommand(Path workDir, String command, List<String> arguments) {
            this.workDir = workDir;
            this.command = command;
            this.arguments = arguments;
        }

        static RestartCommand readStdin() throws IOException {
            Path workDir = null;
            String command = null;
            List<String> arguments = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(System.in, StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.equals("END")) {
                        break;
                    }
                    if (line.startsWith("workdir=")) {
                        workDir = Path.of(line.substring("workdir=".length()));
                    } else if (line.startsWith("command=")) {
                        command = line.substring("command=".length());
                    } else if (line.startsWith("arg=")) {
                        arguments.add(line.substring("arg=".length()));
                    }
                }
            }
            if (workDir == null || command == null || command.isBlank()) {
                return null;
            }
            return new RestartCommand(workDir, command, arguments);
        }

        void launch() throws IOException {
            List<String> commandLine = new ArrayList<>();
            commandLine.add(command);
            commandLine.addAll(arguments);
            ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.directory(workDir.toFile());
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            builder.start();
        }
    }
}
