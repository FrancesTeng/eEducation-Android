package io.agora.education.impl.room.data.response

class EduStreamListRes(
        var count: Int,
        var total: Int,
        var nextId: String,
        var list: MutableList<EduStreamRes>
) {
}

open class EduStreamRes(
        var fromUser: EduFromUserRes,
        var streamUuid: String,
        var streamName: String,
        var videoSourceType: Int,
        var audioSourceType: Int,
        var videoState: Int,
        var audioState: Int,
        var updateTime: Long,
        var state: Int?) {
}

class EduFromUserRes(
        var userUuid: String,
        var userName: String,
        var role: String
) {
}