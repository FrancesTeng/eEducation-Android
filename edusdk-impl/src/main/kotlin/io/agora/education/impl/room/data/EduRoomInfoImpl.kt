package io.agora.education.impl.room.data

import io.agora.education.api.room.data.EduRoomInfo
import io.agora.education.api.room.data.RoomType

internal class EduRoomInfoImpl(val roomType: RoomType, roomUuid: String, roomName: String)
    : EduRoomInfo(roomUuid, roomName) {

}