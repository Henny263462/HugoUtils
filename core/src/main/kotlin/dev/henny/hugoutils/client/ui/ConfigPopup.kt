@file:Suppress("unused")

package dev.henny.hugoutils.client.ui

typealias ConfigPopup = dev.henny.hugoutils.ui.Popup

object PopupManager {
    val active: ConfigPopup? get() = dev.henny.hugoutils.ui.UiOverlays.host.active
    fun open(popup: ConfigPopup) = dev.henny.hugoutils.ui.UiOverlays.host.open(popup)
    fun close() = dev.henny.hugoutils.ui.UiOverlays.host.close()
}
