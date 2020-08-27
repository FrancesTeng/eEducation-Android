package io.agora.education.api.room.data

open class EduRoomInfo(
        /**同一个appId下的房间唯一标示，同时也是rtc、rtm中的channelName*/
        val roomUuid: String,
        val roomName: String
) {
        companion object {
                fun create(roomType: Int, roomUuid: String, roomName: String): EduRoomInfo {
                        val cla = Class.forName("io.agora.education.impl.room.data.EduRoomInfoImpl")
                        val eduRoomInfo = cla.getConstructor(Int::class.java, String::class.java, String::class.java)
                                .newInstance(roomType, roomUuid, roomName) as EduRoomInfo
                        return eduRoomInfo
                }
        }
}
