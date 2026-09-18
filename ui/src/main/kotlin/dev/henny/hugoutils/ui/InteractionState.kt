package dev.henny.hugoutils.ui

class InteractionState {
    val hover = AnimatedFloat(0f, .12f)
    val press = AnimatedFloat(0f, .08f, Easing.EASE_OUT)
    val focus = AnimatedFloat(0f, .14f)

    fun update(hovered: Boolean, pressed: Boolean = false, focused: Boolean = false) {
        hover.animateTo(if (hovered) 1f else 0f)
        press.animateTo(if (pressed) 1f else 0f)
        focus.animateTo(if (focused) 1f else 0f)
        val dt = UiFrame.deltaSeconds
        hover.update(dt)
        press.update(dt)
        focus.update(dt)
    }

    fun pulse() {
        press.snapTo(1f)
        press.animateTo(0f)
    }
}
