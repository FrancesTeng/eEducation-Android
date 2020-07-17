package io.agora.rte

import io.agora.education.api.message.EduTextMessage
import io.agora.rtc.models.ChannelMediaOptions

interface IRteChannel {
    fun join(token: String, uid: Int, mediaOptions: ChannelMediaOptions)

    fun leave()

    fun release()
}
