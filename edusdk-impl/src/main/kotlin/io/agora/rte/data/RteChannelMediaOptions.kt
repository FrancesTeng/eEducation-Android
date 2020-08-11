package io.agora.rte.data

import io.agora.education.api.room.data.RoomMediaOptions
import io.agora.rtc.models.ChannelMediaOptions

internal class RteChannelMediaOptions(autoSubscribeAudio: Boolean, autoSubscribeVideo: Boolean) :
        ChannelMediaOptions() {

    init {
        this.autoSubscribeAudio = autoSubscribeAudio
        this.autoSubscribeVideo = autoSubscribeVideo
    }

    companion object {
        fun build(mediaOptions: RoomMediaOptions): RteChannelMediaOptions {
            return RteChannelMediaOptions(mediaOptions.autoSubscribeAudio, mediaOptions.autoSubscribeVideo)
        }
    }
}