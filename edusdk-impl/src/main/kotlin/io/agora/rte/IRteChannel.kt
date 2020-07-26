package io.agora.rte

import androidx.annotation.NonNull
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtm.ResultCallback

interface IRteChannel {
    fun join(token: String, rtmToken: String, uid: Int, mediaOptions: ChannelMediaOptions, @NonNull callback: ResultCallback<Void>)

    fun leave()

    fun release()
}
