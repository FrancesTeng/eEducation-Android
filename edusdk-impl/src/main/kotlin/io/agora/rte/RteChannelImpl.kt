package io.agora.rte

import io.agora.rtc.IRtcChannelEventHandler
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtm.*

internal class RteChannelImpl(
        channelId: String
) : IRteChannel {
    private val rtmChannelListener = object : RtmChannelListener {
        override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {
            TODO("Not yet implemented")
        }

        /**收到频道内消息(包括频道内的群聊消息和各种房间配置、人员信息、流信息等)*/
        override fun onMessageReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
            TODO("Not yet implemented")
        }

        override fun onMemberJoined(p0: RtmChannelMember?) {
            TODO("Not yet implemented")
        }

        override fun onMemberLeft(p0: RtmChannelMember?) {
            TODO("Not yet implemented")
        }

        override fun onMemberCountUpdated(p0: Int) {
            TODO("Not yet implemented")
        }
    }

    private val rtcChannelEventHandler = object : IRtcChannelEventHandler() {
    }

    private val rtmChannel = RteEngineImpl.rtmClient.createChannel(channelId, rtmChannelListener)
    private val rtcChannel = RteEngineImpl.rtcEngine.createRtcChannel(channelId)

    init {
        rtcChannel.setRtcChannelEventHandler(rtcChannelEventHandler)

    }

    override fun join(rtcToken: String, uid: Int, mediaOptions: ChannelMediaOptions) {

        rtmChannel.join(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(p0: ErrorInfo?) {
                TODO("Not yet implemented")
            }
        })
        rtcChannel.joinChannel(rtcToken, null, uid, ChannelMediaOptions().apply {
            this.autoSubscribeAudio = mediaOptions.autoSubscribeAudio
            autoSubscribeVideo = mediaOptions.autoSubscribeVideo
        })
    }

    override fun leave() {
        rtmChannel.leave(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(p0: ErrorInfo?) {
                TODO("Not yet implemented")
            }
        })
        rtcChannel.leaveChannel()
    }

    override fun release() {
        rtmChannel.release()
        rtcChannel.destroy()
    }
}
