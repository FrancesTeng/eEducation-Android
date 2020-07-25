package io.agora.education.impl.room.data.response

import io.agora.education.api.room.data.EduRoomInfo
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.user.data.request.RoleMuteConfig

/**userUuid: 房间内用户唯一id，同时也是用户加入rtm的uid
 * userToken: 用户token*/
class EduEntryRes(val room: EduEntryRoomRes, val user: EduEntryUserRes) {
}

class EduEntryRoomRes(val roomInfo: EduRoomInfo, val roomState: EduEntryRoomStateRes) {

}

class EduEntryRoomStateRes(val state: Int, val startTime: Long,
                           val muteChat: RoleMuteConfig?, val muteVideo: RoleMuteConfig?,
                           val muteAudio: RoleMuteConfig?) {

}

class EduEntryUserRes(val userUuid: String, val streamUuid: String, val userToken: String,
                      val rtmToken: String, val rtcToken: String, val muteChat: Int) {
}
