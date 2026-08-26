# Contributing

Contributions are welcome through GitHub issues and pull requests.

## Development setup

1. Install a Java 21 JDK.
2. Clone the repository.
3. Run `./gradlew :fabric:build`.
4. Use `./gradlew :fabric:runClient` for local testing.

Use `gradlew.bat` instead of `gradlew` on Windows if needed.

## Pull requests

- Keep changes focused and explain user-visible behavior.
- Match the existing Kotlin and Java style.
- Do not commit generated files, game files, logs, or decompiled Minecraft
  sources.
- Run `./gradlew clean :fabric:build` before submitting.
- Test rendering changes in game, including the feature's disabled and
  unauthorized states.
- Update documentation when behavior or network access changes.

Minecraft and Fabric internals change frequently. Include the tested Minecraft,
Fabric Loader, and Fabric API versions in bug reports and pull requests.

## Licensing

By contributing, you agree that your contribution may be distributed under the
MIT License. Changes based on third-party code must preserve its license and be
recorded in `THIRD_PARTY_NOTICES.md`.
