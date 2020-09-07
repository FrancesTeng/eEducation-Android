package io.agora.education.impl.cmd.bean

import io.agora.education.api.room.data.EduRoomInfo
import io.agora.education.impl.room.data.response.EduRoomInfoRes
import io.agora.education.impl.room.data.response.EduUserRes

/**rtm传送频道消息时，返回数据的数据结构*/
class RtmMsg(
        val fromUser: EduUserRes,
        val fromRoom: EduRoomInfo,
        val message: String,
        val type: Int?
) {
}