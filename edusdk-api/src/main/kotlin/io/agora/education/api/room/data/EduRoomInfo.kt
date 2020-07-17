package io.agora.education.api.room.data

open class EduRoomInfo(
        /**同一个appId下的房间唯一标示，同时也是rtc、rtm中的channelName*/
        val roomUuid: String,
        val roomName: String
)
