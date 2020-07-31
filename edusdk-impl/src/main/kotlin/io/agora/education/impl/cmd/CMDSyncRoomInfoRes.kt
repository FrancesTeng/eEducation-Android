package io.agora.education.impl.cmd

import io.agora.education.api.room.data.EduRoomInfo
import io.agora.education.api.room.data.EduRoomStatus
import io.agora.education.impl.room.data.response.EduEntryRoomStateRes

class CMDSyncRoomInfoRes(
        val roomInfo: EduRoomInfo,
        val roomState: EduEntryRoomStateRes,
        val roomProperties: Any
) {
}