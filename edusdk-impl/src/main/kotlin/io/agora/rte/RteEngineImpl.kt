package io.agora.rte

import android.content.Context
import android.util.Log
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtm.RtmClient
import io.agora.rtm.RtmClientListener
import io.agora.rtm.RtmMessage
import io.agora.rtm.RtmStatusCode

internal object RteEngineImpl : IRteEngine {
    lateinit var rtmClient: RtmClient
        private set
    lateinit var rtcEngine: RtcEngine
        private set
    private val channelMap = mutableMapOf<String, IRteChannel>()

    var eventListener:RteEngineEventListener? = null

    override fun init(context: Context, appId: String) {
        rtmClient = RtmClient.createInstance(context, appId, rtmClientListener)
        rtcEngine = RtcEngine.create(context, appId, rtcEngineEventHandler)
        Log.e("RteEngineImpl", "init")
    }

    override fun createChannel(channelId: String, eventListener: RteChannelEventListener): IRteChannel {
        val rteChannel = RteChannelImpl(channelId, eventListener)
        channelMap[channelId] = rteChannel
        return rteChannel
    }

    operator fun get(channelId: String): IRteChannel? {
        return channelMap[channelId]
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
    }
}
