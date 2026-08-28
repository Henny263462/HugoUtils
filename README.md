# HugoUtils

HugoUtils is a client-side Fabric mod for Hugo SMP. It adds configurable,
depth-tested glow effects for items and players, a custom held-item glint, and
an integrated version of Fast Items.

## Features

- Dropped-item glow that respects terrain depth
- Held-item glow and configurable enchantment glint
- Player glow with allowlist and denylist filters
- Item filters for visual effects
- Fast Items rendering with per-item controls
- Named configuration profiles
- Secure browser sign-in through Minecraft's session server

Features are enabled per Minecraft account by the Hugo SMP service. Without an
active entitlement, the corresponding feature remains off and its controls are
locked. Entitled features can still be enabled or disabled locally.

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

HugoUtils contacts `hugo.henny.dev` to retrieve feature access for the current
Minecraft UUID. The `/login` command also contacts that service to create and
complete a short-lived browser login challenge.

The Minecraft access token is sent only to Mojang's official session server as
part of that verification. It is not sent to Hugo SMP or stored by the mod.
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

- `core`: configuration, UI, access checks, and shared rendering code
- `itemglow`: dropped- and held-item effects
- `playerglow`: player outline rendering
- `fastitems`: adapted Fast Items implementation
- `fabric`: final distributable JAR containing all modules

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. Security
issues should be reported according to [SECURITY.md](SECURITY.md).

## License

HugoUtils is source-available under the
[HugoUtils Source Available License 1.0](LICENSE.txt). It is not an Open Source
license as defined by the Open Source Initiative.

You may view the source, modify it for personal private use, and compile and
use original or privately modified versions. Redistribution of original or
modified HugoUtils is prohibited without prior written permission from the
HugoUtils copyright holders.

Portions derived from Fast Items remain licensed under CC0 1.0 Universal and
are not restricted by the HugoUtils license where CC0 applies. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Previous HugoUtils releases that were already published under the MIT License
remain MIT licensed. This license does not apply retroactively to those
releases.
