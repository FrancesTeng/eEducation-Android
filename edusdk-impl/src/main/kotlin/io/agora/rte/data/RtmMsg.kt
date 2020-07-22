package io.agora.rte.data

import io.agora.education.impl.room.data.response.EduUserRes

/**rtm传送频道消息时，返回数据的数据结构*/
class RtmMsg(
        val fromUser: EduUserRes,
        val message: String,
        val type: Int
) {
}