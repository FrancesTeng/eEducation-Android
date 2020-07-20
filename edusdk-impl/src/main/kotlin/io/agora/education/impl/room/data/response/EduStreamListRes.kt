package io.agora.education.impl.room.data.response

import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.education.impl.user.data.EduUserInfoImpl

class EduStreamListRes(
        var count: Int,
        var total: Int,
        var nextId: String,
        var list: MutableList<EduStreamRes>
) {
}

class EduStreamRes(
        var fromUser: EduFromUserRes,
        var streamUuid: String,
        var streamName: String,
        var videoSourceType: Int,
        var audioSourceType: Int,
        var videoState: Int,
        var audioState: Int,
        var updateTime: Int) {
}

class EduFromUserRes(
        var userId: String,
        var userUuid: String,
        var userName: String,
        var role: String
) {
}