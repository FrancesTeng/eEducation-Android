package io.agora.education.api.stream.data

import io.agora.education.api.user.data.EduUserInfo

class EduStreamEvent(val modifiedStream: EduStreamInfo, val operatorUser: EduUserInfo) {
}