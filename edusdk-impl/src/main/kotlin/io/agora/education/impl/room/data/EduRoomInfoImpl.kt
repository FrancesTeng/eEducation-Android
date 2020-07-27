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

/**请求增强数据时，用于分辨此用户是上线用户还是下线用户*/
enum class OnlineState(var value: Int) {
    Online(1),
    Offline(0)
}