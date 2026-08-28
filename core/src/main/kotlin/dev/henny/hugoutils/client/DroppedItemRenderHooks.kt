package dev.henny.hugoutils.client

import net.minecraft.client.render.entity.state.ItemEntityRenderState

object DroppedItemRenderHooks {
    private var beginCallback: ((ItemEntityRenderState) -> Unit)? = null
    private var endCallback: (() -> Unit)? = null

    fun register(begin: (ItemEntityRenderState) -> Unit, end: () -> Unit) {
        beginCallback = begin
        endCallback = end
    }

    @JvmStatic
    fun begin(state: ItemEntityRenderState) {
        beginCallback?.invoke(state)
    }

    @JvmStatic
    fun end() {
        endCallback?.invoke()
    }
}
