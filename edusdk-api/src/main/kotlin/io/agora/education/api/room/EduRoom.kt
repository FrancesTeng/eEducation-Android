package io.agora.education.api.room

import io.agora.education.api.EduCallback
import io.agora.education.api.board.EduBoard
import io.agora.education.api.record.EduRecord
import io.agora.education.api.room.data.EduRoomInfo
import io.agora.education.api.room.data.EduRoomStatus
import io.agora.education.api.room.data.Property
import io.agora.education.api.room.data.RoomJoinOptions
import io.agora.education.api.room.listener.EduRoomEventListener
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.EduTeacher
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduUserInfo

abstract class EduRoom(
        val roomInfo: EduRoomInfo,
        var roomStatus: EduRoomStatus
) {
    var roomProperties: Map<String, Any>? = mapOf()

    lateinit var localUser: EduUser
    lateinit var board: EduBoard
    lateinit var record: EduRecord

    var eventListener: EduRoomEventListener? = null

    abstract fun joinClassroomAsTeacher(options: RoomJoinOptions, callback: EduCallback<EduTeacher>)

    abstract fun joinClassroomAsStudent(options: RoomJoinOptions, callback: EduCallback<EduStudent>)

    abstract fun getStudentCount(): Int

    abstract fun getTeacherCount(): Int

    abstract fun getStudentList(): MutableList<EduUserInfo>

    abstract fun getTeacherList(): MutableList<EduUserInfo>

    abstract fun getFullStreamList(): MutableList<EduStreamInfo>

    abstract fun getFullUserList(): MutableList<EduUserInfo>

    abstract fun leave()

    abstract fun release()
}
