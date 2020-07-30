package io.agora.education.api.user.listener

import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserInfo

interface EduUserEventListener {
    fun onLocalUserUpdated(userEvent: EduUserEvent)

    fun onLocalStreamAdded(streamInfo: EduStreamEvent)

    fun onLocalStreamUpdated(streamInfo: EduStreamEvent)

    fun onLocalSteamRemoved(streamInfo: EduStreamEvent)
}
