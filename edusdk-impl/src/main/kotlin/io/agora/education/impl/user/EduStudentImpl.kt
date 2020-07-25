package io.agora.education.impl.user

import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduStudentEventListener

internal class EduStudentImpl(
        userInfo: EduUserInfo
) : EduUserImpl(userInfo), EduStudent {

    override fun setEventListener(eventListener: EduStudentEventListener) {
        this.eventListener = eventListener
    }
}
