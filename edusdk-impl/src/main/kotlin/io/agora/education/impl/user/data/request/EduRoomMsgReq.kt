package io.agora.education.impl.user.data.request

/**发送channelMsg*/
internal open class EduRoomMsgReq(val msg: String)

internal class EduRoomChatMsgReq(
        msg: String,
        val type: Int) : EduRoomMsgReq(msg)