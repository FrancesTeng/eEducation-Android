package io.agora.education.impl.cmd

import io.agora.education.impl.room.data.response.EduFromUserRes
import io.agora.education.impl.room.data.response.EduStreamRes
import io.agora.education.impl.room.data.response.EduUserRes

class CMDStreamActionMsg(
        fromUser: EduFromUserRes,
        streamUuid: String,
        streamName: String,
        videoSourceType: Int,
        audioSourceType: Int,
        videoState: Int,
        audioState: Int,
        val action: Int,
        updateTime: Long,
        val operator: EduFromUserRes
) : EduStreamRes(fromUser, streamUuid, streamName, videoSourceType,
        audioSourceType, videoState, audioState, updateTime) {
}