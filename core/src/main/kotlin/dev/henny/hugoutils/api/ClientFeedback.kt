package dev.henny.hugoutils.api

import com.google.gson.JsonObject

data class FeedbackReport(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val status: String,
    val source: String,
    val reply: String?,
    val repliedAt: String?,
    val createdAt: String
) {
    fun kindLabel(): String = when (kind) {
        "feature" -> "Feature"
        "issue" -> "Issue"
        "note" -> "Anmerkung"
        else -> kind
    }

    fun statusLabel(): String = when (status) {
        "open" -> "Offen"
        "in_progress" -> "In Arbeit"
        "resolved" -> "Erledigt"
        "closed" -> "Geschlossen"
        else -> status
    }
}

object ClientFeedback {
    fun parseList(body: JsonObject?): List<FeedbackReport> {
        if (body == null) return emptyList()
        val reports = JsonView.objectsNamed(body, "reports", "feedback", "items").ifEmpty { JsonView.objects(body) }
        return reports.mapNotNull(::parse)
    }

    fun parse(obj: JsonObject): FeedbackReport? {
        val id = JsonView.str(obj, "id") ?: return null
        val report = JsonView.child(obj, "report") ?: obj
        return FeedbackReport(
            id = JsonView.str(report, "id") ?: id,
            kind = JsonView.str(report, "kind", "type") ?: "note",
            title = JsonView.str(report, "title") ?: "Meldung",
            body = JsonView.str(report, "body", "message", "text") ?: "",
            status = JsonView.str(report, "status") ?: "open",
            source = JsonView.str(report, "source") ?: "client",
            reply = JsonView.str(report, "reply", "response"),
            repliedAt = JsonView.str(report, "repliedAt"),
            createdAt = JsonView.str(report, "createdAt", "updatedAt") ?: ""
        )
    }
}
