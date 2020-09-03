package io.agora.education.impl.cmd

import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.RoomStatusEvent
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserInfo

internal class CMDCallbackManager {

    /**回调同步过程中的有效数据；本地和远端数据分开走不同的回调函数*/
    fun callbackValidData(eduRoom: EduRoom) {
//        var validDataArray = dataCache.getValidDataArray()
//
//        /**处理同步过程中的有效数据，过滤出本地数据，并把本地数据从集合中remove掉*/
//        val validLocal = CMDProcessor.processValidData(eduRoom, validDataArray)
//
//        /**数据同步的第二阶段期间，发生改变的本地有效数据*/
//        val validModifiedLocalUsersBySyncing = validLocal[0] as MutableList<EduUserEvent>
//        val validAddedLocalStreamsBySyncing = validLocal[1] as MutableList<EduStreamEvent>
//        val validModifiedLocalStreamsBySyncing = validLocal[2] as MutableList<EduStreamEvent>
//        val validRemovedLocalStreamsBySyncing = validLocal[3] as MutableList<EduStreamEvent>
//
//        validDataArray = dataCache.getValidDataArray()
//        if (validDataArray[0].size > 0) {
//            eduRoom.eventListener?.onRemoteUsersJoined(validDataArray[0] as MutableList<EduUserInfo>, eduRoom)
//        }
//        if (validDataArray[1].size > 0) {
//            eduRoom.eventListener?.onRemoteUserUpdated(validDataArray[1] as MutableList<EduUserEvent>, eduRoom)
//        }
//        validModifiedLocalUsersBySyncing?.forEach {
//            eduRoom.localUser.eventListener?.onLocalUserUpdated(it)
//        }
//        if (validDataArray[2].size > 0) {
//            eduRoom.eventListener?.onRemoteUsersLeft(validDataArray[2] as MutableList<EduUserEvent>, eduRoom)
//        }
//        if (validDataArray[3].size > 0) {
//            eduRoom.eventListener?.onRemoteStreamsAdded(validDataArray[3] as MutableList<EduStreamEvent>, eduRoom)
//        }
//        validAddedLocalStreamsBySyncing?.forEach {
//            eduRoom.localUser.eventListener?.onLocalStreamAdded(it)
//        }
//        if (validDataArray[4].size > 0) {
//            eduRoom.eventListener?.onRemoteStreamsUpdated(validDataArray[4] as MutableList<EduStreamEvent>, eduRoom)
//        }
//        validModifiedLocalStreamsBySyncing?.forEach {
//            eduRoom.localUser.eventListener?.onLocalStreamUpdated(it)
//        }
//        if (validDataArray[5].size > 0) {
//            eduRoom.eventListener?.onRemoteStreamsRemoved(validDataArray[5] as MutableList<EduStreamEvent>, eduRoom)
//        }
//        validRemovedLocalStreamsBySyncing?.forEach {
//            eduRoom.localUser.eventListener?.onLocalStreamRemoved(it)
//        }
//        /**每次增量数据回调完成，都要清空集合*/
//        dataCache.clearValidDataArray()
    }

    fun onRoomStatusChanged(event: RoomStatusEvent, operatorUser: EduUserInfo?, classRoom: EduRoom) {
        classRoom.eventListener?.onRoomStatusChanged(event, operatorUser, classRoom)
    }

    fun onRoomPropertyChanged(classRoom: EduRoom) {
        classRoom.eventListener?.onRoomPropertyChanged(classRoom)
    }

    fun onRoomChatMessageReceived(chatMsg: EduChatMsg, classRoom: EduRoom) {
        classRoom.eventListener?.onRoomChatMessageReceived(chatMsg, classRoom)
    }

    fun onRoomMessageReceived(message: EduMsg, classRoom: EduRoom) {
        classRoom.eventListener?.onRoomMessageReceived(message, classRoom)
    }

    fun onRemoteUsersJoined(users: List<EduUserInfo>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteUsersJoined(users, classRoom)
    }

    fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteStreamsAdded(streamEvents, classRoom)
    }

    fun onRemoteUsersLeft(userEvents: MutableList<EduUserEvent>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteUsersLeft(userEvents, classRoom)
    }

    fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteStreamsRemoved(streamEvents, classRoom)
    }

    fun onRemoteUserPropertiesUpdated(userInfos: MutableList<EduUserInfo>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteUserPropertiesUpdated(userInfos, classRoom)
    }

    fun onRemoteStreamsUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteStreamsUpdated(streamEvents, classRoom)
    }

    fun onRemoteUserUpdated(userEvents: MutableList<EduUserEvent>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteUserUpdated(userEvents, classRoom)
    }

    fun onUserChatMessageReceived(chatMsg: EduChatMsg, classRoom: EduRoom) {
        classRoom.eventListener?.onUserChatMessageReceived(chatMsg, classRoom)
    }

    fun onUserMessageReceived(message: EduMsg, classRoom: EduRoom) {
        classRoom.eventListener?.onUserMessageReceived(message, classRoom)
    }

    fun onLocalUserUpdated(userEvent: EduUserEvent, eduUser: EduUser) {
        eduUser.eventListener?.onLocalUserUpdated(userEvent)
    }

    fun onLocalUserPropertyUpdated(userInfo: EduUserInfo, eduUser: EduUser) {
        eduUser.eventListener?.onLocalUserPropertyUpdated(userInfo)
    }

    fun onLocalStreamAdded(streamEvent: EduStreamEvent, eduUser: EduUser) {
        eduUser.eventListener?.onLocalStreamAdded(streamEvent)
    }

    fun onLocalStreamUpdated(streamEvent: EduStreamEvent, eduUser: EduUser) {
        eduUser.eventListener?.onLocalStreamUpdated(streamEvent)
    }

    fun onLocalStreamRemoved(streamEvent: EduStreamEvent, eduUser: EduUser) {
        eduUser.eventListener?.onLocalStreamRemoved(streamEvent)
    }
}