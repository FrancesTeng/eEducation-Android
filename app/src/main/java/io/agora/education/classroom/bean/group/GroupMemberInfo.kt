package io.agora.education.classroom.bean.group

import io.agora.education.api.user.data.EduBaseUserInfo

class GroupMemberInfo(
        val userInfo: EduBaseUserInfo,
        val integral: Int
) {
    var online: Boolean = false
    var hasAudio: Boolean = false
    var hasVideo: Boolean = false

    /**用户上台*/
    fun online() {
        online = true
        hasAudio = true
        hasVideo = true
    }

    fun offLine() {

    }
}