package io.agora.rte.data

import io.agora.education.impl.room.data.request.RoleConfig
import io.agora.education.impl.user.data.request.RoleMuteConfig

class RtmRoomMuteState(val muteChat: RoleMuteConfig?, val muteVideo: RoleMuteConfig?,
                       val muteAudio: RoleMuteConfig?) {
}