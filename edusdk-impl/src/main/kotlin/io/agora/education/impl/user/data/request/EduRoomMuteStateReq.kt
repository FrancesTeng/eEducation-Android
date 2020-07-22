package io.agora.education.impl.user.data.request

import com.google.gson.Gson
import io.agora.education.api.stream.data.EduAudioState
import io.agora.education.api.stream.data.EduVideoState
import io.agora.education.api.user.data.EduChatState
import io.agora.education.impl.role.data.EduUserRoleStr

/**三个参数的状态只要不是disable就等同于enable*/
class EduRoomMuteStateReq() {
    var muteChat: String? = null
    var muteVideo: String? = null
    var muteAudio: String? = null

    init {
    }

    constructor(chatState: EduVideoState) : this() {
        var chat: MutableMap<String, Int> = mutableMapOf()
        chat[EduUserRoleStr.broadcaster.value] = chatState.value
        chat[EduUserRoleStr.audience.value] = chatState.value
        this.muteChat = Gson().toJson(chat)
    }

    constructor(videoState: EduChatState) : this() {
        var video: MutableMap<String, Int> = mutableMapOf()
        video[EduUserRoleStr.broadcaster.value] = videoState.value
        video[EduUserRoleStr.audience.value] = videoState.value
        this.muteVideo = Gson().toJson(video)
    }

    constructor(audioState: EduAudioState) : this() {
        var audio: MutableMap<String, Int> = mutableMapOf()
        audio[EduUserRoleStr.broadcaster.value] = audioState.value
        audio[EduUserRoleStr.audience.value] = audioState.value
        this.muteAudio = Gson().toJson(audio)
    }
}

/**上边代码中的map需要改为属性*/
class RoleMuteConfig constructor(val host:  String?, val broadcaster: String?, val audience: String?) {

}