# Security Policy

## Supported versions

Security fixes are provided for the latest published release.

## Reporting a vulnerability

Please do not open a public issue for vulnerabilities involving authentication,
session handling, access control, or remote services. Report them privately
through GitHub's security advisory form for this repository.

Include the affected version, reproduction steps, expected impact, and any
relevant logs with credentials and session tokens removed. Reports will be
acknowledged as soon as possible.

## Session handling

The `/login` flow uses the access token already held by the Minecraft client to
call Mojang's official session server. HugoUtils does not send that token to
`hugo.henny.dev`, write it to disk, or include it in logs.

Never share Minecraft access tokens, GitHub tokens, Gradle credentials, or
production environment files in an issue.
