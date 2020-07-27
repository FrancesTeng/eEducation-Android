package io.agora.education.api.message

import io.agora.education.api.user.data.EduUserInfo

open class EduChatMsg(
        fromUser: EduUserInfo,
        message: String,
        timeStamp: Long,
        val type: Int
) : EduMsg(fromUser, message, timeStamp)

enum class EduChatMsgType(var value: Int) {
    Text(1)
}
