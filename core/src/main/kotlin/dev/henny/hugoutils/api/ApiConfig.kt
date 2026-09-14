package dev.henny.hugoutils.api

object ApiConfig {
    const val USER_AGENT = "HugoUtils"
    const val HUGO_BOT_ORIGIN = "https://hugo.henny.dev"
    const val PUBLIC_LOGIN_URL = "$HUGO_BOT_ORIGIN/anmelden"
    const val CLIENT_LOGIN_URL = "$HUGO_BOT_ORIGIN/auth/client-login"
    const val MOJANG_SESSION_JOIN_URL = "https://sessionserver.mojang.com/session/minecraft/join"
    const val GITHUB_API_ORIGIN = "https://api.github.com"
    const val GITHUB_OWNER = "Henny263462"
    const val GITHUB_REPO = "HugoUtils"
    const val GITHUB_RELEASES_URL = "$GITHUB_API_ORIGIN/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
}
