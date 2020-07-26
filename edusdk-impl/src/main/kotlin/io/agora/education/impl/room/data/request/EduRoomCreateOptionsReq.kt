package io.agora.education.impl.room.data.request

import io.agora.education.api.room.data.RoomCreateOptions
import io.agora.education.api.room.data.RoomProperty
import io.agora.education.api.room.data.RoomProperty.Companion.KEY_STUDENT_LIMIT
import io.agora.education.api.room.data.RoomProperty.Companion.KEY_TEACHER_LIMIT
import io.agora.education.api.room.data.RoomType

class RoomCreateOptionsReq constructor() {
    lateinit var roomName: String
    lateinit var roleConfig: RoleConfig

    constructor(roomName: String, roleConfig: RoleConfig) : this() {
        this.roomName = roomName
        this.roleConfig = roleConfig
    }

    companion object {

        fun convertToSelf(roomCreateOptions: RoomCreateOptions): RoomCreateOptionsReq {
            var roomCreateOptionsReq = RoomCreateOptionsReq()
            roomCreateOptionsReq.roomName = roomCreateOptions.roomName
            var mRoleConfig = RoleConfig()

            var roomProperties = roomCreateOptions.properties
            for (roomProperty in roomProperties) {
                modifyRoleConfig(mRoleConfig, roomProperty, roomCreateOptions.roomType)
            }
            roomCreateOptionsReq.roleConfig = mRoleConfig
            return roomCreateOptionsReq
        }

        private fun modifyRoleConfig(roleConfig: RoleConfig, roomProperty: RoomProperty, roomType: RoomType) {
            var teacherLimit = 0
            var studentLimit = 0
            when (roomProperty.key) {
                KEY_TEACHER_LIMIT -> {
                    teacherLimit = roomProperty.value?.toInt() ?: 0
                }
                KEY_STUDENT_LIMIT -> {
                    studentLimit = roomProperty.value?.toInt() ?: 0
                }
            }
            roleConfig.host = LimitConfig(teacherLimit)
            if (roomType == RoomType.LARGE_CLASS) {
                roleConfig.audience = LimitConfig(studentLimit)
            } else {
                roleConfig.broadcaster = LimitConfig(studentLimit)
            }
        }
    }
}

class RoleConfig constructor() {
    lateinit var host: LimitConfig
    lateinit var broadcaster: LimitConfig
    lateinit var audience: LimitConfig

    constructor(host: LimitConfig, broadcaster: LimitConfig, audience: LimitConfig) : this() {
    }
}

class LimitConfig constructor(val limit: Int) {
    var verifyType = VerifyType.NotAllow.value

    constructor(limit: Int, verifyType: Int) : this(limit) {
        this.verifyType = verifyType
    }
}

/**验证类型, 0: 允许匿名, 1: 不允许匿名 */
enum class VerifyType(var value: Int) {
    Allow(0),
    NotAllow(1)
}