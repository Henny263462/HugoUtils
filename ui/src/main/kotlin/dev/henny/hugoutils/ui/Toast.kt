package dev.henny.hugoutils.ui

data class Toast(val message: String, val durationSeconds: Float = 3f, val kind: Kind = Kind.INFO) {
    enum class Kind { INFO, SUCCESS, ERROR }
}
