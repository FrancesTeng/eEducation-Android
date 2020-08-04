package io.agora.education.impl.cmd

import android.util.Log
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
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.request.EduSyncFinished
import io.agora.education.impl.room.data.request.EduSyncStep
import io.agora.rte.RteEngineImpl
import java.util.*

class CMDDispatch {

    companion object {
        /**数据同步期间屏蔽针对room和userStream的改变*/
        private var roomStateChangeEnable: Boolean = true
        private var userStreamChangeEnable: Boolean = true

        /**关闭 数据改变开关，说明因为某种原因需要同步数据
         * 1：join流程
         * 2：断线重连
         * 3：数据超时，重新求情*/
        fun disableDataChangeEnable() {
            roomStateChangeEnable = false
            userStreamChangeEnable = false
        }

        /**数据同步的第二阶段，发生改变的有效数据*/
        private val validOnlineUserList = mutableListOf<EduUserInfo>()
        private val validModifiedUserList = mutableListOf<EduUserEvent>()
        private val validOfflineUserList = mutableListOf<EduUserEvent>()
        private val validAddedStreamList = mutableListOf<EduStreamEvent>()
        private val validModifiedStreamList = mutableListOf<EduStreamEvent>()
        private val validRemovedStreamList = mutableListOf<EduStreamEvent>()

        /**关于本地数据的改变*/
        private val modifiedLocalUserList = mutableListOf<EduUserEvent>()
        private val addedLocalStreamList = mutableListOf<EduStreamEvent>()
        private val modifiedLocalStreamList = mutableListOf<EduStreamEvent>()
        private val removedLocalStreamEvent = mutableListOf<EduStreamEvent>()

        private fun filterMsg(cmdResponseBody: CMDResponseBody<Any>): Boolean {
            var pass = true
            when (cmdResponseBody.cmd) {
                CMDId.RoomStateChange.value, CMDId.RoomMuteStateChange.value -> {
                    pass = roomStateChangeEnable
                }
                CMDId.UserStateChange.value, CMDId.UserJoinOrLeave.value,
                CMDId.StreamStateChange.value -> {
                    pass = userStreamChangeEnable
                }
            }
            return pass
        }

        fun dispatchChannelMsg(text: String, eduRoom: EduRoom, eventListener: EduRoomEventListener?) {
            val cmdResponseBody = Gson().fromJson<CMDResponseBody<Any>>(text, object :
                    TypeToken<CMDResponseBody<CMDRoomState>>() {}.type)
            /**过滤消息*/
            if (!filterMsg(cmdResponseBody)) {
                return
            }
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
                    Log.e("CMDDispatch", "收到频道内聊天消息")
                    val eduMsg = buildEduMsg(text, eduRoom) as EduChatMsg
                    Log.e("CMDDispatch", "构造出eduMsg")
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
                    val validOnlineUsers = CMDDataFuser.addUserWithOnline(rtmInOutMsg.onlineUsers,
                            (eduRoom as EduRoomImpl).getCurUserList(), (eduRoom as EduRoomImpl).getCurRoomType())
                    val validOfflineUsers = CMDDataFuser.filterUserWithOffline(rtmInOutMsg.offlineUsers,
                            (eduRoom as EduRoomImpl).getCurUserList(), (eduRoom as EduRoomImpl).getCurRoomType())
                    /**如果当前正在同步过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                    if ((eduRoom as EduRoomImpl).joinSuccess) {
                        if (validOnlineUsers.size > 0) {
                            eventListener?.onRemoteUsersJoined(validOnlineUsers, eduRoom)
                        }
                        if (validOfflineUsers.size > 0) {
                            /**收到用户离开的通知，需要和本地缓存的流做匹配，移除掉对应用户的流*/
                            val removedStreamEvent = CMDDataFuser.removeStreamWithUserLeave(
                                    validOfflineUsers, eduRoom.getCurStreamList())
                            eventListener?.onRemoteUsersLeft(validOfflineUsers, eduRoom)
                            eventListener?.onRemoteStreamsRemoved(removedStreamEvent, eduRoom)
                        }
                    }
                }
                CMDId.UserStateChange.value -> {
                    val cmdUserStateMsg = Gson().fromJson<CMDResponseBody<CMDUserStateMsg>>(text, object :
                            TypeToken<CMDResponseBody<CMDUserStateMsg>>() {}.type).data
                    val validUserList = CMDDataFuser.modifyUserWithUserStateChange(cmdUserStateMsg,
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
                            Log.e("CMDDispatch", "收到新添加流的通知：${text}")
                            val validAddStreams = CMDDataFuser.addStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                            if ((eduRoom as EduRoomImpl).joinSuccess) {
                                eventListener?.onRemoteStreamsAdded(validAddStreams, eduRoom)
                                /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                                for (element in validAddStreams) {
                                    if (element.modifiedStream.publisher == eduRoom.localUser.userInfo) {
                                        updateLocalStream(element.modifiedStream.hasAudio, element.modifiedStream.hasVideo)
                                        eduRoom.localUser.eventListener?.onLocalStreamAdded(element)
                                    }
                                }
                            }
                        }
                        CMDStreamAction.Modify.value -> {
                            Log.e("CMDDispatch", "收到修改流的通知：${text}")
                            val validModifyStreams = CMDDataFuser.modifyStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                            if ((eduRoom as EduRoomImpl).joinSuccess) {
                                eventListener?.onRemoteStreamsUpdated(validModifyStreams, eduRoom)
                                /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                                for (element in validModifyStreams) {
                                    if (element.modifiedStream.publisher == eduRoom.localUser.userInfo) {
                                        updateLocalStream(element.modifiedStream.hasAudio, element.modifiedStream.hasVideo)
                                        eduRoom.localUser.eventListener?.onLocalStreamUpdated(element)
                                    }
                                }
                            }
                        }
                        CMDStreamAction.Remove.value -> {
                            Log.e("CMDDispatch", "收到移除流的通知：${text}")
                            val validRemoveStreams = CMDDataFuser.removeStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                            if ((eduRoom as EduRoomImpl).joinSuccess) {
                                eventListener?.onRemoteStreamsRemoved(validRemoveStreams, eduRoom)
                                /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                                for (element in validRemoveStreams) {
                                    if (element.modifiedStream.publisher == eduRoom.localUser.userInfo) {
                                        updateLocalStream(element.modifiedStream.hasAudio, element.modifiedStream.hasVideo)
                                        eduRoom.localUser.eventListener?.onLocalStreamRemoved(element)
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
            Log.e("CMDDispatch", "构造buildEduMsg")
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
                /**只要发起数据同步请求就会受到此消息*/
                CMDId.SyncRoomInfo.value -> {
                    Log.e("CMDDispatch", "收到同步房间信息的消息")
                    /**接收到需要同步的房间信息*/
                    val cmdSyncRoomInfoRes = Gson().fromJson<CMDResponseBody<CMDSyncRoomInfoRes>>(text,
                            object : TypeToken<CMDResponseBody<CMDSyncRoomInfoRes>>() {}.type)
                    /**数据同步流程中需要根据requestId判断，此次接收到的数据是否对应于当前请求*/
                    if (cmdResponseBody.requestId == (eduRoom as EduRoomImpl).eduSyncRoomReq.requestId) {
                        val event = CMDDataFuser.syncRoomInfoToEduRoom(cmdSyncRoomInfoRes.data, eduRoom)
                        /**在join成功之后同步数据过程中，如果教室数据发生改变就回调出去*/
                        if (eduRoom.joinSuccess && event != null) {
                            eventListener?.onRoomStatusChanged(event, null, eduRoom)
                        }
                        /**roomInfo同步完成，打开开关*/
                        roomStateChangeEnable = true
                        /**roomInfo同步成功*/
                        Log.e("CMDDispatch", "房间信息同步完成")
                        eduRoom.syncRoomOrAllUserStreamSuccess(
                                true, null, null)
                    }
                }
                /**同步人流数据的消息*/
                CMDId.SyncUsrStreamList.value -> {
                    /**接收到需要同步的人流信息*/
                    val cmdSyncUserStreamRes = Gson().fromJson<CMDResponseBody<CMDSyncUserStreamRes>>(text,
                            object : TypeToken<CMDResponseBody<CMDSyncUserStreamRes>>() {}.type)
                    /**数据同步流程中需要根据requestId判断，此次接收到的数据是否对应于当前请求*/
                    if (cmdResponseBody.requestId == (eduRoom as EduRoomImpl).eduSyncRoomReq.requestId) {
                        val syncUserStreamData = cmdSyncUserStreamRes.data
                        /**第一阶段（属于join流程）（根据nextId全量），如果中间断连，可根据nextId续传;
                         * 第二阶段（不属于join流程）（根据ts增量），如果中间断连，可根据ts续传*/
                        when (syncUserStreamData.step) {
                            EduSyncStep.FIRST.value -> {
                                Log.e("CMDDispatch", "收到同步人流的消息-第一阶段")
                                /**把此部分的全量人流数据同步到本地缓存中*/
                                CMDDataFuser.syncUserStreamListToEduRoomWithFirst(syncUserStreamData, eduRoom)
                                val firstFinished = syncUserStreamData.isFinished == EduSyncFinished.YES.value
                                /**接收到一部分全量数据，就调用一次，目的是为了刷新rtm超时任务*/
                                Log.e("CMDDispatch", "第一阶段同步完成")
                                (eduRoom as EduRoomImpl).syncRoomOrAllUserStreamSuccess(
                                        null, firstFinished, null)
                                /**更新全局的nextId,方便在后续出现异常的时候可以以当前节点为起始步骤继续同步*/
                                (eduRoom as EduRoomImpl).eduSyncRoomReq.nextId = syncUserStreamData.nextId.toString()
                                /**如果步骤一同步完成，则说明join流程中的同步全量人流数据阶段完成，同时还需要把全局的step改为2，
                                 * 防止在步骤二(join流程中的同步增量人流数据阶段)过程出现异常后，再次发起的同步请求中step还是1*/
                                if (firstFinished) {
                                    Log.e("CMDDispatch", "收到同步人流的消息-第一阶段完成")
                                    (eduRoom as EduRoomImpl).eduSyncRoomReq.step = EduSyncStep.SECOND.value
                                }
                            }
                            EduSyncStep.SECOND.value -> {
                                /**增量数据合并到本地缓存中去*/
                                val validDatas = CMDDataFuser.syncUserStreamListToEduRoomWithSecond(
                                        syncUserStreamData, eduRoom)
                                val incrementFinished = syncUserStreamData.isFinished == EduSyncFinished.YES.value
                                /**接收到一部分增量数据，就调用一次，目的是为了刷新rtm超时任务*/
                                if (eduRoom.joinSuccess) {
                                    Log.e("CMDDispatch", "收到同步人流的消息-join成功后的增量")
                                    (eduRoom as EduRoomImpl).interruptRtmTimeout(!incrementFinished)
                                } else {
                                    Log.e("CMDDispatch", "收到同步人流的消息-第二阶段")
                                    Log.e("CMDDispatch", "第二阶段同步完成")
                                    (eduRoom as EduRoomImpl).syncRoomOrAllUserStreamSuccess(
                                            null, incrementFinished, null)
                                }
                                /**更新全局的nextTs,方便在后续出现异常的时候可以以当前节点为起始步骤继续同步*/
                                eduRoom.eduSyncRoomReq.nextTs = syncUserStreamData.nextTs
                                /**获取有效的增量数据*/
                                if (eduRoom.joinSuccess) {
                                    validOnlineUserList.addAll(validDatas[0] as MutableList<EduUserInfo>)
                                    validModifiedUserList.addAll(validDatas[1] as MutableList<EduUserEvent>)
                                    validOfflineUserList.addAll(validDatas[2] as MutableList<EduUserEvent>)
                                    validAddedStreamList.addAll(validDatas[3] as MutableList<EduStreamEvent>)
                                    validModifiedStreamList.addAll(validDatas[4] as MutableList<EduStreamEvent>)
                                    validRemovedStreamList.addAll(validDatas[5] as MutableList<EduStreamEvent>)
                                }
                                if (incrementFinished) {
                                    /**成功加入房间后的全部增量数据需要回调出去*/
                                    if (eduRoom.joinSuccess) {
                                        Log.e("CMDDispatch", "收到同步人流的消息-join成功后的增量-完成")
                                        if (validOnlineUserList.size > 0) {
                                            val list = mutableListOf<EduUserInfo>()
                                            Collections.copy(list, validOnlineUserList)
                                            eduRoom.eventListener?.onRemoteUsersJoined(list, eduRoom)
                                        }
                                        if (validModifiedUserList.size > 0) {
                                            /**判断被修改的数据中是否有本地用户的数据*/
                                            for (element in validModifiedUserList) {
                                                if (element.modifiedUser == eduRoom.localUser.userInfo) {
                                                    validModifiedUserList.remove(element)
                                                    modifiedLocalUserList.add(element)
                                                    val list = mutableListOf<EduUserEvent>()
                                                    Collections.copy(list, modifiedLocalUserList)
                                                    eduRoom.localUser.eventListener?.onLocalUserUpdated(element)
                                                    break
                                                }
                                            }
                                            val list = mutableListOf<EduUserEvent>()
                                            Collections.copy(list, validModifiedUserList)
                                            eduRoom.eventListener?.onRemoteUserUpdated(list, eduRoom)

                                        }
                                        if (validOfflineUserList.size > 0) {
                                            val list = mutableListOf<EduUserEvent>()
                                            Collections.copy(list, validOfflineUserList)
                                            eduRoom.eventListener?.onRemoteUsersLeft(list, eduRoom)
                                        }
                                        if (validAddedStreamList.size > 0) {
                                            /**判断添加的流数据中是否有属于本地用户的流*/
                                            for (element in validAddedStreamList) {
                                                if (element.modifiedStream.publisher == eduRoom.localUser.userInfo) {
                                                    validAddedStreamList.remove(element)
                                                    addedLocalStreamList.add(element)
                                                    val list = mutableListOf<EduStreamEvent>()
                                                    Collections.copy(list, addedLocalStreamList)
                                                    for (event in list) {
                                                        eduRoom.localUser.eventListener?.onLocalStreamAdded(event)
                                                    }
                                                }
                                            }
                                            val list = mutableListOf<EduStreamEvent>()
                                            Collections.copy(list, validAddedStreamList)
                                            eduRoom.eventListener?.onRemoteStreamsAdded(list, eduRoom)
                                        }
                                        if (validModifiedStreamList.size > 0) {
                                            /**判断被更改的流数据中是否有属于本地用户的流*/
                                            for (element in validModifiedStreamList) {
                                                if (element.modifiedStream.publisher == eduRoom.localUser.userInfo) {
                                                    validModifiedStreamList.remove(element)
                                                    modifiedLocalStreamList.add(element)
                                                    val list = mutableListOf<EduStreamEvent>()
                                                    Collections.copy(list, modifiedLocalStreamList)
                                                    for (event in list) {
                                                        eduRoom.localUser.eventListener?.onLocalStreamUpdated(event)
                                                    }
                                                }
                                            }
                                            val list = mutableListOf<EduStreamEvent>()
                                            Collections.copy(list, validModifiedStreamList)
                                            eduRoom.eventListener?.onRemoteStreamsUpdated(list, eduRoom)
                                        }
                                        if (validRemovedStreamList.size > 0) {
                                            /**判断被移除的流数据中是否有属于本地用户的流*/
                                            for (element in validRemovedStreamList) {
                                                if (element.modifiedStream.publisher == eduRoom.localUser.userInfo) {
                                                    validRemovedStreamList.remove(element)
                                                    removedLocalStreamEvent.add(element)
                                                    val list = mutableListOf<EduStreamEvent>()
                                                    Collections.copy(list, removedLocalStreamEvent)
                                                    for (event in list) {
                                                        eduRoom.localUser.eventListener?.onLocalStreamRemoved(event)
                                                    }
                                                }
                                            }
                                            val list = mutableListOf<EduStreamEvent>()
                                            Collections.copy(list, validRemovedStreamList)
                                            eduRoom.eventListener?.onRemoteStreamsRemoved(list, eduRoom)
                                        }
                                        /**此次增量统计完成，清空集合*/
                                        validOnlineUserList.clear()
                                        validModifiedUserList.clear()
                                        validOfflineUserList.clear()
                                        validAddedStreamList.clear()
                                        validModifiedStreamList.clear()
                                        validRemovedStreamList.clear()
                                    } else {
                                        Log.e("CMDDispatch", "收到同步人流的消息-第二阶段完成")
                                    }
                                    /**userStream同步完成，打开开关*/
                                    userStreamChangeEnable = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}