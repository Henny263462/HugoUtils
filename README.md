# HugoUtils

HugoUtils is a client-side Fabric mod for Hugo SMP. It adds configurable,
depth-tested glow effects for items and players, a custom held-item glint, and
an integrated version of Fast Items.

HugoUtils is **open source under the Apache License 2.0**.

## Features

- Dropped-item glow that respects terrain depth
- Held-item glow and configurable enchantment glint
- Player glow with allowlist and denylist filters
- Item filters for visual effects
- Fast Items rendering with per-item controls
- Named configuration profiles
- Optional browser sign-in for account and future Market features
- Built-in updates from official GitHub Releases

Local visual features, filters, profiles, and configuration work without an
account, without HugoBot, and without a network connection. Login is optional
and is not required to open or use the configuration UI.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.3 or newer
- Fabric API
- Fabric Language Kotlin
- Java 21

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API and Fabric Language Kotlin.
3. Place the HugoUtils JAR in the Minecraft `mods` directory.
4. Open the configuration screen with Right Shift.

Download release files only from this repository or the official CurseForge
project once it is available. Every GitHub release includes a
`SHA256SUMS.txt` file.

## Network access and privacy

Local HugoUtils features do not require login and do not require HugoBot.

### HugoBot

The separate HugoBot service at `hugo.henny.dev` may be used for:

- authentication;
- account functionality;
- Market;
- future server-backed functionality.

The `/login` command contacts HugoBot to create and complete a short-lived
browser login challenge. This license does not automatically cover HugoBot.

Market features may require network access and authentication. Market
authorization is enforced by HugoBot, not by local client checks.

### GitHub

When update checking is enabled, HugoUtils contacts GitHub for:

- official release update checks;
- release downloads;
- checksum downloads.

Update checking can be disabled in the Updates settings. Updates are never
delivered by HugoBot.

### Mojang

The `/login` flow sends the Minecraft access token only to Mojang's official
session server. That token is not sent to HugoBot, written to disk, or stored
in logs.

There is no analytics or advertising code.

## Building

Clone the repository and run:

```shell
./gradlew clean :fabric:build
```

On Windows:

```powershell
.\gradlew.bat clean :fabric:build
```

The installable JAR is written to `fabric/build/libs/`. Builds require Java 21.
The Gradle wrapper is included in the repository.

## Project structure

- `core`: configuration, UI, optional HugoBot clients, updates, and shared rendering code
- `itemglow`: dropped- and held-item effects
- `playerglow`: player outline rendering
- `fastitems`: adapted Fast Items implementation
- `fabric`: final distributable JAR containing all modules

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. Security
issues should be reported according to [SECURITY.md](SECURITY.md).

Forks, issues, branches, and pull requests are welcome.

## License

HugoUtils is open source under the [Apache License 2.0](LICENSE.txt).

Subject to Apache-2.0, you may:

- use;
- modify;
- compile;
- fork;
- redistribute;
- create derivative projects;
- submit pull requests.

Derivative projects should use their own primary name and branding. See
[TRADEMARKS.md](TRADEMARKS.md) for branding guidance. That policy does not
modify the rights Apache-2.0 grants to the source code.

Portions derived from Fast Items remain licensed under CC0-1.0. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Do not treat Fast Items as
Apache-2.0 licensed.

The separate HugoBot backend is not automatically covered by this license.
