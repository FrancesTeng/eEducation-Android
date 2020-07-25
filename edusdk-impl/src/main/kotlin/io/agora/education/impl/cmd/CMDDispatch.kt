package io.agora.education.impl.cmd

import com.google.gson.Gson
import io.agora.Convert
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.RoomStatusEvent
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.room.listener.EduRoomEventListener
import io.agora.education.api.stream.data.EduAudioState
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.impl.room.EduRoomImpl

class CMDDispatch {

    companion object {
        fun dispatch(cmdResponseBody: CMDResponseBody<String>, eduRoom: EduRoom, eventListener: EduRoomEventListener?) {
            when (cmdResponseBody.cmd) {
                CMDId.RoomStateChange.value -> {
                    /**课堂状态发生改变*/
                    val rtmRoomState = Gson().fromJson(cmdResponseBody.data, CMDRoomState::class.java)
                    eduRoom.roomStatus.courseState = Convert.convertRoomState(rtmRoomState.state)
                    eduRoom.roomStatus.startTime = rtmRoomState.startTime
                    val operator = Convert.convertUserInfo(rtmRoomState.operator, (eduRoom as EduRoomImpl).getCurRoomType())
                    eventListener?.onRoomStatusChanged(RoomStatusEvent.COURSE_STATE, operator, eduRoom)
                }
                CMDId.RoomMuteStateChange.value -> {
                    val rtmRoomMuteState = Gson().fromJson(cmdResponseBody.data, CMDRoomMuteState::class.java)
                    when ((eduRoom as EduRoomImpl).getCurRoomType()) {
                        RoomType.ONE_ON_ONE, RoomType.SMALL_CLASS -> {
                            /**判断本次更改是否包含针对学生的全部静音;(一对一和小班课学生的角色是broadcaster)*/
                            val broadcasterMuteChat = rtmRoomMuteState.muteChat?.broadcaster
                            broadcasterMuteChat?.let {
                                eduRoom.roomStatus.isStudentChatAllowed = broadcasterMuteChat.toInt() == EduAudioState.Open.value
                            }
                            /**
                             * roomStatus中仅定义了isStudentChatAllowed来标识是否全员静音；没有属性来标识是否全员禁摄像头和麦克风；
                             * 需要确定
                             * */
                        }
                        RoomType.LARGE_CLASS -> {
                            /**判断本次更改是否包含针对学生的全部静音;(大班课学生的角色是audience)*/
                            val audienceMuteChat = rtmRoomMuteState.muteChat?.audience
                            audienceMuteChat?.let {
                                eduRoom.roomStatus.isStudentChatAllowed = audienceMuteChat.toInt() == EduAudioState.Open.value
                            }
                        }
                    }
                    val operator = Convert.convertUserInfo(rtmRoomMuteState.operator, (eduRoom as EduRoomImpl).getCurRoomType())
                    eventListener?.onRoomStatusChanged(RoomStatusEvent.STUDENT_CHAT, operator, eduRoom)
                }
                CMDId.ChannelMsgReceived.value -> {
                    val rtmMsg = Gson().fromJson(cmdResponseBody.data, RtmMsg::class.java)
                    val fromUser = Convert.convertUserInfo(rtmMsg.fromUser, (eduRoom as EduRoomImpl).getCurRoomType())
                    val eduMsg = EduMsg(fromUser, rtmMsg.message, cmdResponseBody.ts)
                    eventListener?.onRoomMessageReceived(eduMsg, eduRoom)
                }
                CMDId.ChannelCustomMsgReceived.value -> {
                }
                CMDId.UserJoinOrLeave.value -> {
                    val rtmInOutMsg = Gson().fromJson(cmdResponseBody.data, RtmUserInOutMsg::class.java)

                    /**根据回调数据，维护本地存储的流列表，并返回有效数据*/
                    val validOnlineUsers = CMDProcessor.addUserWithOnline(rtmInOutMsg.onlineUsers,
                            (eduRoom as EduRoomImpl).getCurUserList(), (eduRoom as EduRoomImpl).getCurRoomType())
                    val validOfflineUsers = CMDProcessor.filterUserWithOffline(rtmInOutMsg.offlineUsers,
                            (eduRoom as EduRoomImpl).getCurUserList(), (eduRoom as EduRoomImpl).getCurRoomType())
                    if (validOnlineUsers.size > 0) {
                        eventListener?.onRemoteUsersJoined(validOnlineUsers, eduRoom)
                    }
                    if (validOfflineUsers.size > 0) {
                        eventListener?.onRemoteUsersLeft(validOfflineUsers, eduRoom)
                    }
                }
                CMDId.UserStateChange.value -> {
                    val cmdUserStateMsg = Gson().fromJson(cmdResponseBody.data, CMDUserStateMsg::class.java)
                    val validUserList = CMDProcessor.modifyUserWithUserStateChange(cmdUserStateMsg,
                            (eduRoom as EduRoomImpl).getCurUserList(), (eduRoom as EduRoomImpl).getCurRoomType())
                    eventListener?.onRemoteUserUpdated(validUserList, eduRoom)
                    /**判断有效的数据中是否有本地用户的数据,有则处理并回调*/
                    for (element in validUserList) {
                        if (element.modifiedUser.userUuid == eduRoom.localUser.userInfo.userUuid) {
                            eduRoom.localUser.eventListener?.onLocalUserUpdated(element.modifiedUser,
                                    element.operatorUser)
                        }
                    }
                }
                CMDId.StreamStateChange.value -> {
                    val cmdStreamActionMsg = Gson().fromJson(cmdResponseBody.data, CMDStreamActionMsg::class.java)
                    /**根据回调数据，维护本地存储的用户列表*/
                    when (cmdStreamActionMsg.action) {
                        CMDStreamAction.Add.value -> {
                            val validAddStreams = CMDProcessor.addStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            eventListener?.onRemoteStreamsAdded(validAddStreams, eduRoom)
                            /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                            for (element in validAddStreams) {
                                if(element.modifiedStream.publisher.userUuid == eduRoom.localUser.userInfo.userUuid) {
                                    eduRoom.localUser.eventListener?.onLocalStreamAdded(
                                            element.modifiedStream, element.operatorUser)
                                }
                            }
                        }
                        CMDStreamAction.Modify.value -> {
                            val validModifyStreams = CMDProcessor.modifyStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            eventListener?.onRemoteStreamsUpdated(validModifyStreams, eduRoom)
                            /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                            for (element in validModifyStreams) {
                                if(element.modifiedStream.publisher.userUuid == eduRoom.localUser.userInfo.userUuid) {
                                    eduRoom.localUser.eventListener?.onLocalStreamUpdated(
                                            element.modifiedStream, element.operatorUser)
                                }
                            }
                        }
                        CMDStreamAction.Remove.value -> {
                            val validRemoveStreams = CMDProcessor.removeStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            eventListener?.onRemoteStreamsRemoved(validRemoveStreams, eduRoom)
                            /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                            for (element in validRemoveStreams) {
                                if(element.modifiedStream.publisher.userUuid == eduRoom.localUser.userInfo.userUuid) {
                                    eduRoom.localUser.eventListener?.onLocalSteamRemoved(
                                            element.modifiedStream, element.operatorUser)
                                }
                            }
                        }
                    }
                }
                CMDId.BoardRoomStateChange.value -> {

                }
                CMDId.BoardUserStateChange.value -> {

                }
            }
        }
    }
}