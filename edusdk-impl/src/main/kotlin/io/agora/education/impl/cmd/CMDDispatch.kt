package io.agora.education.impl.cmd

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.Convert
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.RoomStatusEvent
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.room.listener.EduRoomEventListener
import io.agora.education.api.stream.data.EduAudioState
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.impl.room.EduRoomImpl
import io.agora.rte.RteEngineImpl

class CMDDispatch {

    companion object {
        fun dispatchChannelMsg(text: String, eduRoom: EduRoom, eventListener: EduRoomEventListener?) {
            val cmdResponseBody = Gson().fromJson<CMDResponseBody<Any>>(text, object :
                    TypeToken<CMDResponseBody<CMDRoomState>>() {}.type)
            when (cmdResponseBody.cmd) {
                CMDId.RoomStateChange.value -> {
                    /**课堂状态发生改变*/
                    val rtmRoomState = Gson().fromJson<CMDResponseBody<CMDRoomState>>(text, object :
                            TypeToken<CMDResponseBody<CMDRoomState>>() {}.type).data
                    eduRoom.roomStatus.courseState = Convert.convertRoomState(rtmRoomState.state)
                    eduRoom.roomStatus.startTime = rtmRoomState.startTime
                    val operator = Convert.convertUserInfo(rtmRoomState.operator, (eduRoom as EduRoomImpl).getCurRoomType())
                    eventListener?.onRoomStatusChanged(RoomStatusEvent.COURSE_STATE, operator, eduRoom)
                }
                CMDId.RoomMuteStateChange.value -> {
                    val rtmRoomMuteState = Gson().fromJson<CMDResponseBody<CMDRoomMuteState>>(text, object :
                            TypeToken<CMDResponseBody<CMDRoomState>>() {}.type).data
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
                    /**频道内的聊天消息*/
                    val eduMsg = buildEduMsg(text, eduRoom) as EduChatMsg
                    eventListener?.onRoomChatMessageReceived(eduMsg, eduRoom)
                }
                CMDId.ChannelCustomMsgReceived.value -> {
                    /**频道内自定义消息(可以是用户的自定义的信令)*/
                    val eduMsg = buildEduMsg(text, eduRoom)
                    eventListener?.onRoomMessageReceived(eduMsg, eduRoom)
                }
                CMDId.UserJoinOrLeave.value -> {
                    val rtmInOutMsg = Gson().fromJson<CMDResponseBody<RtmUserInOutMsg>>(text, object :
                            TypeToken<CMDResponseBody<RtmUserInOutMsg>>() {}.type).data

                    /**根据回调数据，维护本地存储的流列表，并返回有效数据*/
                    val validOnlineUsers = CMDProcessor.addUserWithOnline(rtmInOutMsg.onlineUsers,
                            (eduRoom as EduRoomImpl).getCurUserList(), (eduRoom as EduRoomImpl).getCurRoomType())
                    val validOfflineUsers = CMDProcessor.filterUserWithOffline(rtmInOutMsg.offlineUsers,
                            (eduRoom as EduRoomImpl).getCurUserList(), (eduRoom as EduRoomImpl).getCurRoomType())
                    /**如果当前正在同步过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                    if ((eduRoom as EduRoomImpl).joinSuccess) {
                        if (validOnlineUsers.size > 0) {
                            eventListener?.onRemoteUsersJoined(validOnlineUsers, eduRoom)
                        }
                        if (validOfflineUsers.size > 0) {
                            /**收到用户离开的通知，需要和本地缓存的流做匹配，移除掉对应用户的流*/
                            val removedStreamEvent = CMDProcessor.removeStreamWithUserLeave(
                                    validOfflineUsers, eduRoom.getCurStreamList())
                            eventListener?.onRemoteUsersLeft(validOfflineUsers, eduRoom)
                            eventListener?.onRemoteStreamsRemoved(removedStreamEvent, eduRoom)
                        }
                    }
                }
                CMDId.UserStateChange.value -> {
                    val cmdUserStateMsg = Gson().fromJson<CMDResponseBody<CMDUserStateMsg>>(text, object :
                            TypeToken<CMDResponseBody<CMDUserStateMsg>>() {}.type).data
                    val validUserList = CMDProcessor.modifyUserWithUserStateChange(cmdUserStateMsg,
                            (eduRoom as EduRoomImpl).getCurUserList(), (eduRoom as EduRoomImpl).getCurRoomType())
                    /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                    if ((eduRoom as EduRoomImpl).joinSuccess) {
                        eventListener?.onRemoteUserUpdated(validUserList, eduRoom)
                        /**判断有效的数据中是否有本地用户的数据,有则处理并回调*/
                        for (element in validUserList) {
                            if (element.modifiedUser.userUuid == eduRoom.localUser.userInfo.userUuid) {
                                eduRoom.localUser.eventListener?.onLocalUserUpdated(EduUserEvent(element.modifiedUser,
                                        element.operatorUser))
                            }
                        }
                    }
                }
                CMDId.StreamStateChange.value -> {
                    val cmdStreamActionMsg = Gson().fromJson<CMDResponseBody<CMDStreamActionMsg>>(text, object :
                            TypeToken<CMDResponseBody<CMDStreamActionMsg>>() {}.type).data
                    /**根据回调数据，维护本地存储的流列表*/
                    when (cmdStreamActionMsg.action) {
                        CMDStreamAction.Add.value -> {
                            val validAddStreams = CMDProcessor.addStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                            if ((eduRoom as EduRoomImpl).joinSuccess) {
                                eventListener?.onRemoteStreamsAdded(validAddStreams, eduRoom)
                                /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                                for (element in validAddStreams) {
                                    if (element.modifiedStream.publisher.userUuid == eduRoom.localUser.userInfo.userUuid) {
                                        updateLocalStream(element.modifiedStream.hasAudio, element.modifiedStream.hasVideo)
                                        eduRoom.localUser.eventListener?.onLocalStreamAdded(element)
                                    }
                                }
                            }
                        }
                        CMDStreamAction.Modify.value -> {
                            val validModifyStreams = CMDProcessor.modifyStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                            if ((eduRoom as EduRoomImpl).joinSuccess) {
                                eventListener?.onRemoteStreamsUpdated(validModifyStreams, eduRoom)
                                /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                                for (element in validModifyStreams) {
                                    if (element.modifiedStream.publisher.userUuid == eduRoom.localUser.userInfo.userUuid) {
                                        updateLocalStream(element.modifiedStream.hasAudio, element.modifiedStream.hasVideo)
                                        eduRoom.localUser.eventListener?.onLocalStreamUpdated(element)
                                    }
                                }
                            }
                        }
                        CMDStreamAction.Remove.value -> {
                            val validRemoveStreams = CMDProcessor.removeStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                            if ((eduRoom as EduRoomImpl).joinSuccess) {
                                eventListener?.onRemoteStreamsRemoved(validRemoveStreams, eduRoom)
                                /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                                for (element in validRemoveStreams) {
                                    if (element.modifiedStream.publisher.userUuid == eduRoom.localUser.userInfo.userUuid) {
                                        updateLocalStream(element.modifiedStream.hasAudio, element.modifiedStream.hasVideo)
                                        eduRoom.localUser.eventListener?.onLocalSteamRemoved(element)
                                    }
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

        private fun buildEduMsg(text: String, eduRoom: EduRoom): EduMsg {
            val cmdResponseBody = Gson().fromJson<CMDResponseBody<RtmMsg>>(text, object :
                    TypeToken<CMDResponseBody<RtmMsg>>() {}.type)
            val rtmMsg = cmdResponseBody.data
            val fromUser = Convert.convertUserInfo(rtmMsg.fromUser, (eduRoom as EduRoomImpl).getCurRoomType())
            return if (rtmMsg.type != null) {
                EduChatMsg(fromUser, rtmMsg.message, cmdResponseBody.timestamp, rtmMsg.type)
            } else {
                EduMsg(fromUser, rtmMsg.message, cmdResponseBody.timestamp)
            }
        }

        private fun updateLocalStream(hasAudio: Boolean, hasVideo: Boolean) {
            if (hasAudio || hasVideo) {
                RteEngineImpl.rtcEngine.enableLocalAudio(hasAudio)
                RteEngineImpl.rtcEngine.enableLocalVideo(hasVideo)
                RteEngineImpl.rtcEngine.muteLocalAudioStream(!hasAudio)
                RteEngineImpl.rtcEngine.muteLocalVideoStream(!hasVideo)
            }
        }

        fun dispatchPeerMsg(text: String, eduRoom: EduRoom, eventListener: EduRoomEventListener?) {
            val cmdResponseBody = Gson().fromJson<CMDResponseBody<Any>>(text, object :
                    TypeToken<CMDResponseBody<Any>>() {}.type)
            when (cmdResponseBody.cmd) {
                CMDId.PeerMsgReceived.value -> {
                    /**点对点的聊天消息*/
                    val eduMsg = buildEduMsg(text, eduRoom) as EduChatMsg
                    eventListener?.onUserChatMessageReceived(eduMsg, eduRoom)
                }
                CMDId.PeerCustomMsgReceived.value -> {
                    /**点对点的自定义消息(可以是用户自定义的信令)*/
                    val eduMsg = buildEduMsg(text, eduRoom)
                    eventListener?.onUserMessageReceived(eduMsg, eduRoom)
                }
            }
        }
    }
}