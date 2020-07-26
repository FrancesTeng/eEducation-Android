package io.agora.rte

import androidx.annotation.NonNull
import io.agora.rtc.Constants
import io.agora.rtc.Constants.ERR_OK
import io.agora.rtc.IRtcChannelEventHandler
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtm.*

internal class RteChannelImpl(
        channelId: String,
        private var eventListener: RteChannelEventListener?
) : IRteChannel {

    private val rtmChannelListener = object : RtmChannelListener {
        override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {
            TODO("Not yet implemented")
        }

        /**收到频道内消息(包括频道内的群聊消息和各种房间配置、人员信息、流信息等)*/
        override fun onMessageReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
            eventListener?.onChannelMsgReceived(p0, p1)
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

    /**rtm登录成功的标志*/
    private var rtmLoginSuccess = false

    init {
        rtcChannel.setRtcChannelEventHandler(rtcChannelEventHandler)
    }

    override fun join(rtcToken: String, rtmToken: String, uid: Int, mediaOptions: ChannelMediaOptions,
                      @NonNull callback: ResultCallback<Void>) {
        val rtcCode = rtcChannel.joinChannel(rtcToken, null, uid, ChannelMediaOptions().apply {
            this.autoSubscribeAudio = mediaOptions.autoSubscribeAudio
            autoSubscribeVideo = mediaOptions.autoSubscribeVideo
        })
        /**rtm不能重复登录*/
        if (!rtmLoginSuccess) {
            RteEngineImpl.rtmClient.login(rtmToken, uid.toString(), object : ResultCallback<Void> {
                override fun onSuccess(p0: Void?) {
                    rtmLoginSuccess = true
                    joinRtmChannel(rtcCode, callback)
                }

                override fun onFailure(p0: ErrorInfo?) {
                    rtmLoginSuccess = false
                    callback.onFailure(p0)
                }
            })
        } else {
            joinRtmChannel(rtcCode, callback)
        }
    }

    private fun joinRtmChannel(rtcCode: Int, @NonNull callback: ResultCallback<Void>) {
        rtmChannel.join(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                if (rtcCode == ERR_OK) {
                    callback.onSuccess(p0)
                } else {
                    callback.onFailure(ErrorInfo(rtcCode))
                }
            }

            override fun onFailure(p0: ErrorInfo?) {
                callback.onFailure(p0)
            }
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
