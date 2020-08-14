package io.agora.rte

import android.content.Context
import android.util.Log
import io.agora.rtc.Constants
import io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import io.agora.rtm.RtmClient
import io.agora.rtm.RtmClientListener
import io.agora.rtm.RtmMessage

internal object RteEngineImpl : IRteEngine {
    lateinit var rtmClient: RtmClient
        private set
    lateinit var rtcEngine: RtcEngine
        private set
    private val channelMap = mutableMapOf<String, IRteChannel>()

    var eventListener: RteEngineEventListener? = null

    /**rtm登录成功的标志*/
    var rtmLoginSuccess = false

    override fun init(context: Context, appId: String) {
        rtmClient = RtmClient.createInstance(context, appId, rtmClientListener)
        rtcEngine = RtcEngine.create(context, appId, rtcEngineEventHandler)
        Log.e("RteEngineImpl", "init")
//        rtcEngine.setParameters("{\"rtc.log_filter\": 65535}")
    }

    override fun createChannel(channelId: String, eventListener: RteChannelEventListener): IRteChannel {
        val rteChannel = RteChannelImpl(channelId, eventListener)
        channelMap[channelId] = rteChannel
        return rteChannel
    }

    operator fun get(channelId: String): IRteChannel? {
        return channelMap[channelId]
    }

    fun setClientRole(channelId: String, role: Int) {
        if(channelMap.isNotEmpty()) {
            (channelMap[channelId] as RteChannelImpl).rtcChannel.setClientRole(role)
        }
    }

    fun publish(channelId: String): Int {
        if(channelMap.isNotEmpty()) {
            return (channelMap[channelId] as RteChannelImpl).rtcChannel.publish()
        }
        return -1
    }

    private val rtmClientListener = object : RtmClientListener {
        override fun onTokenExpired() {
        }

        override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {
        }

        /**RTE连接质量发生改变*/
        override fun onConnectionStateChanged(p0: Int, p1: Int) {
            eventListener?.onConnectionStateChanged(p0, p1)
        }

        /**收到私聊消息 peerMsg*/
        override fun onMessageReceived(p0: RtmMessage?, p1: String?) {
            eventListener?.onPeerMsgReceived(p0, p1)
        }
    }

    private val rtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
            eventListener?.onNetworkQuality(uid, txQuality, rxQuality)
        }

        override fun onError(err: Int) {
            Log.e("RteEngineImpl", String.format("onError code %d message %s", err, RtcEngine.getErrorDescription(err)))
        }

        override fun onWarning(warn: Int) {
            super.onWarning(warn)
            Log.e("RteEngineImpl", String.format("onWarning code %d message %s", warn, RtcEngine.getErrorDescription(warn)));
        }

        override fun onClientRoleChanged(oldRole: Int, newRole: Int) {
            super.onClientRoleChanged(oldRole, newRole)
            Log.e("RteEngineImpl", "onClientRoleChanged, $oldRole, $newRole")
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Log.e("RteEngineImpl", String.format("onJoinChannelSuccess channel %s uid %d", channel, uid));
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Log.e("RteEngineImpl", "onUserJoined->$uid")
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
            Log.e("RteEngineImpl", "onRemoteVideoStateChanged->$uid, state->$state, reason->$reason")
        }
    }
}
