package io.agora.rte

import android.content.Context

interface IRteEngine {
    fun init(context: Context, appId: String)

    fun createChannel(channelId: String, eventListener: RteChannelEventListener): IRteChannel

    fun setClientRole(channelId: String, role: Int)

    fun publish(channelId: String): Int

    fun unpublish(channelId: String): Int

    fun updateLocalStream(hasAudio: Boolean, hasVideo: Boolean)
}
