package io.agora.education.impl.room.data.response

class EduUserListRes(
        var count: Int,
        var total: Int,
        var nextId: String,
        var list: MutableList<EduUserRes>
) {
}

class EduUserRes(var userUuid: String,
                 var userName: String,
                 var role: String,
                 var updateTime: Int) {
}