package io.agora.raisehand

import io.agora.education.api.base.EduError

interface AgoraEduCoVideoListener {

    fun onApplyCoVideoComplete()

    fun onApplyCoVideoFailed(error: EduError)

    fun onCancelCoVideoSuccess()

    fun onCancelCoVideoFailed(error: EduError)

    fun onCoVideoAborted()

    fun onCoVideoAccepted()

    fun onCoVideoRejected()

}