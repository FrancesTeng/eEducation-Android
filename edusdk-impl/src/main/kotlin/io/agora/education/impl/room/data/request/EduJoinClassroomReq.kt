package io.agora.education.impl.room.data.request

/**加入房间时的请求参数
 * @param userUuid 房间内用户唯一ID, 同时也是用户加入RTM的UID
 * @param streamUuid 默认流id，如果不设，则使用userUuid*/
class EduJoinClassroomReq(
        val userUuid:   String,
        val userName:   String,
        val role:       String,
        val streamUuid: String
) {
}