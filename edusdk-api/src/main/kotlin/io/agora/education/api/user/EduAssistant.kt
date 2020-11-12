package io.agora.education.api.user

import io.agora.education.api.user.listener.EduAssistantEventListener

interface EduAssistant : EduUser {
    fun setEventListener(eventListener: EduAssistantEventListener)
}
