package io.agora.education.api.message

open class EduMsg(
        val userUuid: String,
        val userName: String,
        val message: String,
        val timeStamp: Int
)
