package dev.henny.hugoutils.client.rtp

object RtpMenus {
    fun isRtpTitle(title: String): Boolean {
        val text = normalize(title)
        if (text.isEmpty()) return false
        return text == "rtp" ||
            text.startsWith("rtp ") ||
            text.endsWith(" rtp") ||
            text.contains(" rtp ") ||
            text.contains("random teleport") ||
            text.contains("random tp") ||
            text.contains("zufallstp") ||
            text.contains("zufalls tp") ||
            text.contains("zufallsteleport") ||
            text.contains("zufalls teleport")
    }

    fun isRtpItem(name: String): Boolean {
        val text = normalize(name)
        if (text.isEmpty()) return false
        return isRtpTitle(text) || text.startsWith("rtp") || text.contains(" rtp")
    }

    fun normalize(raw: String): String =
        raw.replace(FORMATTING, "")
            .lowercase()
            .replace(PUNCTUATION, " ")
            .replace(WHITESPACE, " ")
            .trim()

    private val FORMATTING = Regex("§.")
    private val PUNCTUATION = Regex("[^a-z0-9äöüß ]+")
    private val WHITESPACE = Regex("\\s+")
}
