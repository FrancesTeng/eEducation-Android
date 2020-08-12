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
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.impl.cmd.bean.*
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

        private fun filterMsg(cmdResponseBody: CMDResponseBody<Any>): Boolean {
            var pass = true
            when (cmdResponseBody.cmd) {
                CMDId.RoomStateChange.value, CMDId.RoomMuteStateChange.value, CMDId.RoomPropertyChanged.value -> {
                    pass = roomStateChangeEnable
                }
                CMDId.UserStateChange.value, CMDId.UserJoinOrLeave.value, CMDId.StreamStateChange.value,
                CMDId.UserPropertiedChanged.value -> {
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
                CMDId.RoomPropertyChanged.value -> {
                    Log.e("CMDDispatch", "收到roomProperty改变的RTM:" + text)
                    val properties = Gson().fromJson<CMDResponseBody<Map<String, Any>>>(text, object :
                            TypeToken<CMDResponseBody<Map<String, Any>>>() {}.type).data
                    /**把变化的属性更新到本地*/
                    eduRoom.roomProperties = properties
                    /**通知用户房间属性发生改变*/
                    Log.e("CMDDispatch", "把收到的roomProperty回调出去")
                    eventListener?.onRoomPropertyChanged(eduRoom)
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
                    Log.e("CMDDispatch", "收到用户进入或离开的通知:" + text)
                    /**根据回调数据，维护本地存储的流列表，并返回有效数据*/
                    val validOnlineUsers = CMDDataMergeProcessor.addUserWithOnline(rtmInOutMsg.onlineUsers,
                            (eduRoom as EduRoomImpl).getCurUserList(), eduRoom.getCurRoomType())
                    val validOfflineUsers = CMDDataMergeProcessor.filterUserWithOffline(rtmInOutMsg.offlineUsers,
                            eduRoom.getCurUserList(), eduRoom.getCurRoomType())
                    /**如果当前正在同步过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                    synchronized(eduRoom.joinSuccess) {
                        if (eduRoom.joinSuccess) {
                            /**人员进出会携带着各自可能存在的流信息*/
                            if (validOnlineUsers.size > 0) {
                                eventListener?.onRemoteUsersJoined(validOnlineUsers, eduRoom)
                            }
                            if (validOfflineUsers.size > 0) {
                                /**收到用户离开的通知，需要和本地缓存的流做匹配，移除掉对应用户的流*/
                                val removedStreamEvent = CMDDataMergeProcessor.removeStreamWithUserLeave(
                                        validOfflineUsers, eduRoom.getCurStreamList())
                                eventListener?.onRemoteUsersLeft(validOfflineUsers, eduRoom)
                                eventListener?.onRemoteStreamsRemoved(removedStreamEvent, eduRoom)
                            }
                        }
                    }
                }
                CMDId.UserStateChange.value -> {
                    val cmdUserStateMsg = Gson().fromJson<CMDResponseBody<CMDUserStateMsg>>(text, object :
                            TypeToken<CMDResponseBody<CMDUserStateMsg>>() {}.type).data
                    val validUserList = CMDDataMergeProcessor.updateUserWithUserStateChange(cmdUserStateMsg,
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
                CMDId.UserPropertiedChanged.value -> {
                    val cmdUserPropertyRes = Gson().fromJson<CMDResponseBody<CMDUserPropertyRes>>(text, object :
                            TypeToken<CMDResponseBody<CMDUserPropertyRes>>() {}.type).data
                    val updatedUserInfo = CMDDataMergeProcessor.updateUserPropertyWithChange(cmdUserPropertyRes,
                            (eduRoom as EduRoomImpl).getCurUserList());
                    updatedUserInfo?.let {
                        if (updatedUserInfo == eduRoom.localUser.userInfo) {
                            eduRoom.localUser.eventListener?.onLocalUserPropertyUpdated(it)
                        } else {
                            /**远端用户property发生改变如何回调出去*/
                            val userInfos = Collections.singletonList(updatedUserInfo)
                            eduRoom.eventListener?.onRemoteUserPropertiesUpdated(userInfos, eduRoom)
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
                            val validAddStreams = CMDDataMergeProcessor.addStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            Log.e("CMDDispatch", "有效新添加流大小：" + validAddStreams.size)
                            /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                            synchronized(eduRoom.joinSuccess) {
                                if (eduRoom.joinSuccess) {
                                    /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                                    val iterable = validAddStreams.iterator()
                                    while (iterable.hasNext()) {
                                        val element = iterable.next()
                                        val streamInfo = element.modifiedStream
                                        if (streamInfo.publisher == eduRoom.localUser.userInfo) {
                                            updateLocalStream(streamInfo)
                                            Log.e("CMDDispatch", "join成功，把新添加的本地流回调出去")
                                            eduRoom.localUser.eventListener?.onLocalStreamAdded(element)
                                            iterable.remove()
                                        }
                                    }
                                    if (validAddStreams.size > 0) {
                                        Log.e("CMDDispatch", "join成功，把新添加远端流回调出去")
                                        eventListener?.onRemoteStreamsAdded(validAddStreams, eduRoom)
                                    } else {
                                    }
                                } else {
                                    eduRoom.addedStreams.addAll(validAddStreams)
                                }
                            }
                        }
                        CMDStreamAction.Modify.value -> {
                            Log.e("CMDDispatch", "收到修改流的通知：${text}")
                            val validModifyStreams = CMDDataMergeProcessor.updateStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            Log.e("CMDDispatch", "有效修改流大小：" + validModifyStreams.size)
                            /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                            if ((eduRoom as EduRoomImpl).joinSuccess) {
                                /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                                val iterable = validModifyStreams.iterator()
                                while (iterable.hasNext()) {
                                    val element = iterable.next()
                                    if (element.modifiedStream.publisher == eduRoom.localUser.userInfo) {
                                        updateLocalStream(element.modifiedStream)
                                        Log.e("CMDDispatch", "join成功，把发生改变的本地流回调出去")
                                        eduRoom.localUser.eventListener?.onLocalStreamUpdated(element)
                                        iterable.remove()
                                    }
                                }
                                if (validModifyStreams.size > 0) {
                                    Log.e("CMDDispatch", "join成功，把发生改变的远端流回调出去")
                                    eventListener?.onRemoteStreamsUpdated(validModifyStreams, eduRoom)
                                }
                            } else {
                                eduRoom.modifiedStreams.addAll(validModifyStreams)
                            }
                        }
                        CMDStreamAction.Remove.value -> {
                            Log.e("CMDDispatch", "收到移除流的通知：${text}")
                            val validRemoveStreams = CMDDataMergeProcessor.removeStreamWithAction(cmdStreamActionMsg,
                                    (eduRoom as EduRoomImpl).getCurStreamList(), (eduRoom as EduRoomImpl).getCurRoomType())
                            /**如果当前正在加入房间的过程中，不回调数据;只保证更新的数据合并到集合中即可*/
                            if (eduRoom.joinSuccess) {
                                /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                                val iterable = validRemoveStreams.iterator()
                                while (iterable.hasNext()) {
                                    val element = iterable.next()
                                    if (element.modifiedStream.publisher == eduRoom.localUser.userInfo) {
                                        updateLocalStream(element.modifiedStream)
                                        eduRoom.localUser.eventListener?.onLocalStreamRemoved(element)
                                        iterable.remove()
                                    }
                                }
                                if(validRemoveStreams.size > 0) {
                                    Log.e("CMDDispatch", "join成功，把被移除的远端流回调出去")
                                    eventListener?.onRemoteStreamsRemoved(validRemoveStreams, eduRoom)
                                }
                            }
                            else {
                                eduRoom.removedStreams.addAll(validRemoveStreams)
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

        fun updateLocalStream(streamInfo: EduStreamInfo) {
            if (streamInfo.hasAudio || streamInfo.hasVideo) {
                RteEngineImpl.rtcEngine.enableLocalAudio(streamInfo.hasAudio)
                RteEngineImpl.rtcEngine.enableLocalVideo(streamInfo.hasVideo)
                RteEngineImpl.rtcEngine.muteLocalAudioStream(!streamInfo.hasAudio)
                RteEngineImpl.rtcEngine.muteLocalVideoStream(!streamInfo.hasVideo)
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
                    Log.e("CMDDispatch", "收到同步房间信息的消息:" + text)
                    /**接收到需要同步的房间信息*/
                    val cmdSyncRoomInfoRes = Gson().fromJson<CMDResponseBody<CMDSyncRoomInfoRes>>(text,
                            object : TypeToken<CMDResponseBody<CMDSyncRoomInfoRes>>() {}.type)
                    /**数据同步流程中需要根据requestId判断，此次接收到的数据是否对应于当前请求*/
                    if (cmdResponseBody.requestId == (eduRoom as EduRoomImpl).eduSyncRoomReq.requestId) {
                        val event = CMDDataMergeProcessor.syncRoomInfoToEduRoom(cmdSyncRoomInfoRes.data, eduRoom)
                        synchronized(eduRoom.joinSuccess) {
                            /**在join成功之后同步数据过程中，如果教室数据发生改变就回调出去*/
                            if (eduRoom.joinSuccess && event != null) {
                                eventListener?.onRoomStatusChanged(event, null, eduRoom)
                            }
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
                        val syncUserStreamData: CMDSyncUserStreamRes = cmdSyncUserStreamRes.data
                        /**第一阶段（属于join流程）（根据nextId全量），如果中间断连，可根据nextId续传;
                         * 第二阶段（不属于join流程）（根据ts增量），如果中间断连，可根据ts续传*/
                        when (syncUserStreamData.step) {
                            EduSyncStep.FIRST.value -> {
                                Log.e("CMDDispatch", "收到同步人流的消息-第一阶段:" + text)
                                /**把此部分的全量人流数据同步到本地缓存中*/
                                CMDDataMergeProcessor.syncUserStreamListToEduRoomWithFirst(syncUserStreamData, eduRoom)
                                val firstFinished = syncUserStreamData.isFinished == EduSyncFinished.YES.value
                                /**接收到一部分全量数据，就调用一次，目的是为了刷新rtm超时任务*/
                                eduRoom.syncRoomOrAllUserStreamSuccess(null,
                                        firstFinished, null)
                                /**更新全局的nextId,方便在后续出现异常的时候可以以当前节点为起始步骤继续同步*/
                                eduRoom.eduSyncRoomReq.nextId = syncUserStreamData.nextId
                                /**如果步骤一同步完成，则说明join流程中的同步全量人流数据阶段完成，同时还需要把全局的step改为2，
                                 * 防止在步骤二(join流程中的同步增量人流数据阶段)过程出现异常后，再次发起的同步请求中step还是1*/
                                if (firstFinished) {
                                    Log.e("CMDDispatch", "收到同步人流的消息-第一阶段完成")
                                    eduRoom.eduSyncRoomReq.step = EduSyncStep.SECOND.value
                                }
                            }
                            EduSyncStep.SECOND.value -> {
                                Log.e("CMDDispatch", "收到同步人流的消息-第二阶段:" + text)
                                /**增量数据合并到本地缓存中去*/
                                val validDatas = CMDDataMergeProcessor.syncUserStreamListToEduRoomWithSecond(
                                        syncUserStreamData, eduRoom)
                                val incrementFinished = syncUserStreamData.isFinished == EduSyncFinished.YES.value
                                synchronized(eduRoom.joinSuccess) {
                                    /**接收到一部分增量数据，就调用一次，目的是为了刷新rtm超时任务*/
                                    if (eduRoom.joinSuccess) {
                                        Log.e("CMDDispatch", "收到同步人流的消息-join成功后的增量")
                                        (eduRoom as EduRoomImpl).interruptRtmTimeout(!incrementFinished)
                                    } else {
                                        if (incrementFinished) {
                                            Log.e("CMDDispatch", "收到同步人流的消息-第二阶段完成")
                                            (eduRoom as EduRoomImpl).syncRoomOrAllUserStreamSuccess(
                                                    null, null, incrementFinished)
                                        }
                                    }
                                    /**更新全局的nextTs,方便在后续出现异常的时候可以以当前节点为起始步骤继续同步*/
                                    eduRoom.eduSyncRoomReq.nextTs = syncUserStreamData.nextTs
                                    /**获取有效的增量数据*/
                                    if (eduRoom.joinSuccess) {
                                        CMDCallbackManager.addValidDataBySyncing(validDatas)
                                    }
                                    if (incrementFinished) {
                                        /**成功加入房间后的全部增量数据需要回调出去*/
                                        if (eduRoom.joinSuccess) {
                                            Log.e("CMDDispatch", "收到同步人流的消息-join成功后的增量-完成")
                                            CMDCallbackManager.callbackValidData(eduRoom)
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
}