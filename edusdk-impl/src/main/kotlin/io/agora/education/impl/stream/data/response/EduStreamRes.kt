package io.agora.education.impl.stream.data.response

import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.data.EduUserInfo

class EduStreamRes constructor(var streamId: String,
                               var streamUuid: String, var streamName: String,
                               var videoSourceType: VideoSourceType,
                               var audioSourceType: Int = 1,
                               var hasVideo: Boolean, var hasAudio: Boolean,
                               var fromUser: EduUserInfo) {
}