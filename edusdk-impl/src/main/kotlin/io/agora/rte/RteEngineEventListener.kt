package io.agora.rte

import io.agora.rtm.RtmMessage

interface RteEngineEventListener {

    /**网络质量发生改变*/
    fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int)

    /**RTE连接质量发生改变*/
    fun onConnectionStateChanged(p0: Int, p1: Int)

    /**收到私聊消息 peerMsg*/
    fun onPeerMsgReceived(p0: RtmMessage?, p1: String?)
}
