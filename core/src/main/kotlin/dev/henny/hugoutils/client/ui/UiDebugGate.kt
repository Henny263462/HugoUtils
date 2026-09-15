package dev.henny.hugoutils.client.ui

object UiDebugGate {
    fun isEnabled(developmentEnvironment: Boolean, configured: Boolean): Boolean =
        developmentEnvironment || configured
}
