# Security Policy

## Supported versions

Security fixes are provided for the latest published release.

## Reporting a vulnerability

Please do not open a public issue for vulnerabilities involving authentication,
session handling, update installation, or remote services. Report them privately
through GitHub's security advisory form for this repository.

Include the affected version, reproduction steps, expected impact, and any
relevant logs with credentials and session tokens removed. Reports will be
acknowledged as soon as possible.

## Session handling

The `/login` flow uses the access token already held by the Minecraft client to
call Mojang's official session server. HugoUtils does not send that token to
HugoBot, write it to disk, or include it in logs.

Local features do not require this login flow.

## Updates

Official updates are downloaded only from GitHub Releases for
`Henny263462/HugoUtils` and are verified with SHA-256 checksums from
`SHA256SUMS.txt` before installation.

Never share Minecraft access tokens, GitHub tokens, Gradle credentials, or
production environment files in an issue.
