package io.agora.education.impl.room.data

import io.agora.education.api.room.data.EduRoomInfo
import io.agora.education.api.room.data.RoomType

internal class EduRoomInfoImpl(val roomType: RoomType, roomUuid: String, roomName: String)
    : EduRoomInfo(roomUuid, roomName) {

}

/**请求增量数据时，是否包含下线用户*/
enum class IncludeOffline(var value: Int) {
    NoInclude(0),
    Include(1)
}

/**请求增量数据时，用于分辨此用户是有效还是无效*/
enum class ValidState(var value: Int) {
    Valid(1),
    Invalid(0)
}