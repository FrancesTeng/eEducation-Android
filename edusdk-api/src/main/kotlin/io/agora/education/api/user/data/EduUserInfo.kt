package io.agora.education.api.user.data

enum class EduUserRole(var value: Int) {
    TEACHER(1),
    STUDENT(2)
}

open class EduUserInfo(
        val userUuid: String,
        val userName: String,
        val role: EduUserRole
)

enum class EduChatState(var value: Int) {
    Enable(0),
    Disable(1)
}