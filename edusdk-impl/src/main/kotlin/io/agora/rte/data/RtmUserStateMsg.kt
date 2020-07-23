package io.agora.rte.data

import io.agora.education.impl.room.data.response.EduUserRes

class RtmUserStateMsg(
        val userUuid: String,
        val userName: String,
        val role: String,
        val muteChat: Int,
        val updateTime: Long,
        val operator: EduUserRes
) {

}