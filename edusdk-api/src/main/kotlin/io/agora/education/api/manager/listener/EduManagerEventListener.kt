package io.agora.education.api.manager.listener

import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.statistics.ConnectionState
import io.agora.education.api.statistics.ConnectionStateChangeReason

interface EduManagerEventListener {

    fun onUserMessageReceived(message: EduMsg, classRoom: EduRoom)

    fun onUserChatMessageReceived(chatMsg: EduChatMsg, classRoom: EduRoom)

    fun onConnectionStateChanged(state: ConnectionState, reason: ConnectionStateChangeReason, classRoom: EduRoom)
}