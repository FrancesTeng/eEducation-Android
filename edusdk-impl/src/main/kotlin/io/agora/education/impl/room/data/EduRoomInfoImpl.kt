package io.agora.education.impl.room.data

import io.agora.education.api.room.data.EduRoomInfo

internal class EduRoomInfoImpl(val roomId: String?, roomUuid: String, roomName: String)
    : EduRoomInfo(roomUuid, roomName) {

}