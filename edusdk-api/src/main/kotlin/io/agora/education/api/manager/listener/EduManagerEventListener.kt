package io.agora.education.api.manager.listener

import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.statistics.ConnectionState
import io.agora.education.api.statistics.ConnectionStateChangeReason

interface EduManagerEventListener {

    fun onUserMessageReceived(message: EduMsg)

    fun onUserChatMessageReceived(chatMsg: EduChatMsg)

    fun onConnectionStateChanged(state: ConnectionState, reason: ConnectionStateChangeReason)
}