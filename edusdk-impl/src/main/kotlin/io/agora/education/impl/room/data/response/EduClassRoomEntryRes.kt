package io.agora.education.impl.room.data.response

/**userUuid: 房间内用户唯一id，同时也是用户加入rtm的uid
 * userToken: 用户token*/
class EduClassRoomEntryRes constructor(var roomUuid: String, var userUuid: String,
                                       var streamUuid: String, var userToken: String,
                                       var rtmToken: String, var rtcToken: String) {
}