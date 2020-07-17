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
        override fun onActiveSpeaker(rtcChannel: RtcChannel?, uid: Int) {
            TODO("Not yet implemented")
        }

        override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onRemoteSubscribeFallbackToAudioOnly(rtcChannel: RtcChannel?, uid: Int, isFallbackOrRecover: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onRtcStats(rtcChannel: RtcChannel?, stats: IRtcEngineEventHandler.RtcStats?) {
            TODO("Not yet implemented")
        }

        override fun onConnectionLost(rtcChannel: RtcChannel?) {
            TODO("Not yet implemented")
        }

        override fun onFirstRemoteAudioFrame(rtcChannel: RtcChannel?, uid: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onSubscribeAudioStateChanged(rtcChannel: RtcChannel?, uid: Int, oldstate: Int, newstate: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onChannelError(rtcChannel: RtcChannel?, err: Int) {
            TODO("Not yet implemented")
        }

        override fun onRemoteVideoStats(rtcChannel: RtcChannel?, stats: IRtcEngineEventHandler.RemoteVideoStats?) {
            TODO("Not yet implemented")
        }

        override fun onPublishAudioStateChanged(rtcChannel: RtcChannel?, oldstate: Int, newstate: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onStreamMessage(rtcChannel: RtcChannel?, uid: Int, streamId: Int, data: ByteArray?) {
            TODO("Not yet implemented")
        }

        override fun onChannelWarning(rtcChannel: RtcChannel?, warn: Int) {
            TODO("Not yet implemented")
        }

        override fun onFirstRemoteVideoFrame(rtcChannel: RtcChannel?, uid: Int, width: Int, height: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onChannelMediaRelayStateChanged(rtcChannel: RtcChannel?, state: Int, code: Int) {
            TODO("Not yet implemented")
        }

        override fun onRequestToken(rtcChannel: RtcChannel?) {
            TODO("Not yet implemented")
        }

        override fun onTranscodingUpdated(rtcChannel: RtcChannel?) {
            TODO("Not yet implemented")
        }

        override fun onClientRoleChanged(rtcChannel: RtcChannel?, oldRole: Int, newRole: Int) {
            TODO("Not yet implemented")
        }

        override fun onRtmpStreamingStateChanged(rtcChannel: RtcChannel?, url: String?, state: Int, errCode: Int) {
            TODO("Not yet implemented")
        }

        override fun onRemoteAudioStats(rtcChannel: RtcChannel?, stats: IRtcEngineEventHandler.RemoteAudioStats?) {
            TODO("Not yet implemented")
        }

        override fun onSubscribeVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, oldstate: Int, newstate: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onStreamUnpublished(rtcChannel: RtcChannel?, url: String?) {
            TODO("Not yet implemented")
        }

        override fun onRejoinChannelSuccess(rtcChannel: RtcChannel?, uid: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onUserJoined(rtcChannel: RtcChannel?, uid: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onStreamMessageError(rtcChannel: RtcChannel?, uid: Int, streamId: Int, error: Int, missed: Int, cached: Int) {
            TODO("Not yet implemented")
        }

        override fun onTokenPrivilegeWillExpire(rtcChannel: RtcChannel?, token: String?) {
            TODO("Not yet implemented")
        }

        override fun onStreamPublished(rtcChannel: RtcChannel?, url: String?, error: Int) {
            TODO("Not yet implemented")
        }

        override fun onUserOffline(rtcChannel: RtcChannel?, uid: Int, reason: Int) {
            TODO("Not yet implemented")
        }

        override fun onNetworkQuality(rtcChannel: RtcChannel?, uid: Int, txQuality: Int, rxQuality: Int) {
            TODO("Not yet implemented")
        }

        override fun onStreamInjectedStatus(rtcChannel: RtcChannel?, url: String?, uid: Int, status: Int) {
            TODO("Not yet implemented")
        }

        override fun onVideoSizeChanged(rtcChannel: RtcChannel?, uid: Int, width: Int, height: Int, rotation: Int) {
            TODO("Not yet implemented")
        }

        override fun onChannelMediaRelayEvent(rtcChannel: RtcChannel?, code: Int) {
            TODO("Not yet implemented")
        }

        override fun onJoinChannelSuccess(rtcChannel: RtcChannel?, uid: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onUserMuteAudio(rtcChannel: RtcChannel?, uid: Int, muted: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onFirstRemoteAudioDecoded(rtcChannel: RtcChannel?, uid: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onLeaveChannel(rtcChannel: RtcChannel?, stats: IRtcEngineEventHandler.RtcStats?) {
            TODO("Not yet implemented")
        }

        override fun onConnectionStateChanged(rtcChannel: RtcChannel?, state: Int, reason: Int) {
            TODO("Not yet implemented")
        }

        override fun onRemoteAudioStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }

        override fun onPublishVideoStateChanged(rtcChannel: RtcChannel?, oldstate: Int, newstate: Int, elapsed: Int) {
            TODO("Not yet implemented")
        }
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
