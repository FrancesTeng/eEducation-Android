package io.agora.education.impl.cmd.bean

import io.agora.education.api.room.data.Property
import io.agora.education.api.user.data.EduBaseUserInfo
import io.agora.education.impl.room.data.response.EduFromUserRes

class CMDUserPropertyRes(
        val fromUser: EduBaseUserInfo,
        val userProperties: Map<String, Any>
) {
}