package io.agora.education.api.user.listener

import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserInfo

interface EduUserEventListener {
    fun onLocalUserUpdated(eduUserInfo: EduUserInfo, operatorUser: EduUserInfo)

    fun onLocalStreamAdded(stream: EduStreamInfo, operatorUser: EduUserInfo)

    fun onLocalStreamUpdated(stream: EduStreamInfo, operatorUser: EduUserInfo)

    fun onLocalSteamRemoved(stream: EduStreamInfo, operatorUser: EduUserInfo)
}
