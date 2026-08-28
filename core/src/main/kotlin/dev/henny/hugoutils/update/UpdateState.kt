package dev.henny.hugoutils.update

enum class UpdateState {
    UNKNOWN,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    VERIFYING,
    READY_TO_INSTALL,
    INSTALLING,
    ERROR
}

data class UpdateStatus(
    val state: UpdateState = UpdateState.UNKNOWN,
    val installedVersion: String? = null,
    val availableVersion: String? = null,
    val message: String? = null,
    val restartSupported: Boolean = false,
    val progress: Float = 0f
)
