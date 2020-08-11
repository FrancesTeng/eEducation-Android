package io.agora.rte

import android.util.Log
import androidx.annotation.NonNull
import io.agora.rtc.Constants.ERR_OK
import io.agora.rtc.IRtcChannelEventHandler
import io.agora.rtc.RtcChannel
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rte.RteEngineImpl.rtmLoginSuccess
import io.agora.rtm.*
import io.agora.rtm.RtmStatusCode.LeaveChannelError.LEAVE_CHANNEL_ERR_USER_NOT_LOGGED_IN

internal class RteChannelImpl(
        channelId: String,
        private var eventListener: RteChannelEventListener?
) : IRteChannel {

    private val rtmChannelListener = object : RtmChannelListener {
        override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {

        }

        /**收到频道内消息(包括频道内的群聊消息和各种房间配置、人员信息、流信息等)*/
        override fun onMessageReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
            eventListener?.onChannelMsgReceived(p0, p1)
        }

        override fun onMemberJoined(p0: RtmChannelMember?) {

        }

        override fun onMemberLeft(p0: RtmChannelMember?) {

        }

        override fun onMemberCountUpdated(p0: Int) {

        }
    }

    private val rtcChannelEventHandler = object : IRtcChannelEventHandler() {
        override fun onClientRoleChanged(rtcChannel: RtcChannel?, oldRole: Int, newRole: Int) {
            super.onClientRoleChanged(rtcChannel, oldRole, newRole)
            Log.e("RteChannelImpl", rtcChannel?.channelId() + "," + oldRole + "," + newRole)
        }

        override fun onJoinChannelSuccess(rtcChannel: RtcChannel?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(rtcChannel, uid, elapsed)
            Log.e("RteChannelImpl", String.format("onJoinChannelSuccess channel %s uid %d", rtcChannel, uid));
        }

        override fun onUserJoined(rtcChannel: RtcChannel?, uid: Int, elapsed: Int) {
            super.onUserJoined(rtcChannel, uid, elapsed)
            Log.e("RteChannelImpl", "onUserJoined->$uid")
        }

        override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteVideoStateChanged(rtcChannel, uid, state, reason, elapsed)
            Log.e("RteChannelImpl", "onRemoteVideoStateChanged->$uid, state->$state, reason->$reason")
        }
    }

    private val rtmChannel = RteEngineImpl.rtmClient.createChannel(channelId, rtmChannelListener)
    val rtcChannel = RteEngineImpl.rtcEngine.createRtcChannel(channelId)
    
    init {
        rtcChannel.setRtcChannelEventHandler(rtcChannelEventHandler)
    }

    override fun join(rtcToken: String, rtmToken: String, rtcUid: Long, rtmUid: String,
                      mediaOptions: ChannelMediaOptions, @NonNull callback: ResultCallback<Void>) {
        val uid = (rtcUid and 0xffffffffL)
        val rtcCode = rtcChannel.joinChannel(rtcToken, null, uid.toInt(), mediaOptions)
        /**rtm不能重复登录*/
        if (!rtmLoginSuccess) {
            RteEngineImpl.rtmClient.login(rtmToken, rtmUid, object : ResultCallback<Void> {
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
                Log.e("RteChannelImpl", "成功离开RTM频道")
            }

            override fun onFailure(p0: ErrorInfo?) {
                Log.e("RteChannelImpl", "离开RTM频道失败:${p0?.errorDescription}")
            }
        })
        rtcChannel.leaveChannel()
    }

    override fun release() {
        RteEngineImpl.rtmClient.logout(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                rtmLoginSuccess = false
                Log.e("RteEngineImpl", "成功退出RTM")
            }

            override fun onFailure(p0: ErrorInfo?) {
                Log.e("RteEngineImpl", "退出RTM失败:${p0?.errorDescription}")
                if (p0?.errorCode == LEAVE_CHANNEL_ERR_USER_NOT_LOGGED_IN) {
                    rtmLoginSuccess = false
                }
            }
        })
        rtmChannel.release()
        rtcChannel.destroy()
    }
}
