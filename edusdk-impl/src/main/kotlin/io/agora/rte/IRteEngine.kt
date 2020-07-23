package io.agora.rte

import android.content.Context

interface IRteEngine {
    fun init(context: Context, appId: String)

    fun createChannel(channelId: String, eventListener: RteChannelEventListener): IRteChannel
}
