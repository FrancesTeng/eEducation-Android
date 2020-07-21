package io.agora.education.impl.room.data.request

/**加入房间时的请求参数
 * @param streamUuid 默认流id，如果不设，则使用userUuid*/
class EduJoinClassroomReq(
        val userName:   String,
        val role:       String,
        val streamUuid: String
) {
}