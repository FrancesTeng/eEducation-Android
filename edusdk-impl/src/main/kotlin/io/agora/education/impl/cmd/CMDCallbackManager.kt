package io.agora.education.impl.cmd

import io.agora.education.api.room.listener.EduRoomEventListener
import io.agora.education.api.user.listener.EduStudentEventListener
import io.agora.education.impl.room.EduRoomImpl

internal class CMDCallbackManager(
        val eduRoomImpl: EduRoomImpl,
        val roomEventListener: EduRoomEventListener,
        val studentEventListener: EduStudentEventListener) {

}