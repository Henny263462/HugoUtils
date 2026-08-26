package dev.henny.hugoutils.client

import net.minecraft.client.render.entity.state.ItemEntityRenderState

/**
 * Bridge that lets feature modules observe the rendering of dropped item entities.
 *
 * Item Glow registers its begin/end callbacks here. Fast Items wraps its custom
 * billboard render path with [begin]/[end], because it cancels the vanilla
 * renderer before Item Glow's own mixin wrapper can run.
 */
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
