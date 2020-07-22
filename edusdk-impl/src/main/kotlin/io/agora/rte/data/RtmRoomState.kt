package io.agora.rte.data

import io.agora.education.impl.room.data.response.EduUserRes

/**rtm通知room状态发生改变时，返回数据的数据结构*/
class RtmRoomState(
        /**房间状态 1开始 0结束*/
        val state: Int,
        val startTime: Int,
        val operator: EduUserRes
) {
}