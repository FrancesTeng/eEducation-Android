package io.agora.education.impl.cmd

import io.agora.Convert
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.RoomStatusEvent
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.EduAudioState
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.EduVideoState
import io.agora.education.api.user.data.EduChatState
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.request.EduSyncFinished
import io.agora.education.impl.room.data.request.EduSyncStep
import io.agora.education.impl.room.data.response.EduFromUserRes
import io.agora.education.impl.room.data.response.EduUserRes
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.data.EduUserInfoImpl

class CMDDataFuser {
    companion object {

        /**调用此函数之前须确保first和second代表的是同一个用户
         *
         * 比较first的数据是否比second的更为接近当前时间(即找出一个最新数据)
         * @return > 0（first > second）
         *         !(> 0) first <= second*/
        private fun compareUserInfoTime(first: EduUserInfo, second: EduUserInfo): Long {
            /**判断更新时间是否为空(为空的有可能是原始数据)*/
            if ((first as EduUserInfoImpl).updateTime == null) {
                return -1
            }
            if ((second as EduUserInfoImpl).updateTime == null) {
                return first.updateTime!!
            }
            /**最终判断出最新数据*/
            return first.updateTime!!.minus(second.updateTime!!)
        }

        /**operator有可能为空(说明用户自己就是操作者)，我们需要把当前用户设置为操作者*/
        private fun getOperator(operator: Any?, userInfo: EduUserInfo, roomType: RoomType):
                EduUserInfo {
            /**operator为空说明操作者是自己*/
            var operatorUser: EduUserInfo? = null
            operator?.let {
                if (operator is EduUserRes) {
                    operatorUser = Convert.convertUserInfo(operator, roomType)
                } else if (operator is EduFromUserRes) {
                    operatorUser = Convert.convertUserInfo(operator, roomType)
                }
            }
            if (operatorUser == null) {
                operatorUser = userInfo
            }
            return operatorUser!!
        }

        /**从 {@param userInfoList} 中过滤掉 离开课堂的用户 {@param offLineUserList}*/
        fun filterUserWithOffline(offLineUserList: MutableList<OffLineUserInfo>,
                                  userInfoList: MutableList<EduUserInfo>, roomType: RoomType):
                MutableList<EduUserEvent> {
            val validUserInfoList = mutableListOf<EduUserEvent>()
            synchronized(userInfoList) {
                for (element in offLineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val userInfo1: EduUserInfo = EduUserInfoImpl(element.userUuid, element.userName, role,
                            element.muteChat == EduChatState.Allow.value, element.updateTime)
                    if (userInfoList.contains(userInfo1)) {
                        val index = userInfoList.indexOf(userInfo1)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = userInfoList[index]
                        /**找出最新数据并替换*/
                        if (compareUserInfoTime(userInfo1, userInfo2) > 0) {
                            /**剔除掉被过滤掉的用户*/
                            userInfoList.removeAt(index)
                            /**构造userEvent并返回*/
                            val operator = getOperator(element.operator, userInfo1, roomType)
                            val userEvent = EduUserEvent(userInfo1, operator)
                            validUserInfoList.add(userEvent)
                        }
                    }
                }
            }
            return validUserInfoList
        }

        fun addUserWithOnline(onlineUserList: MutableList<EduUserRes>,
                              userInfoList: MutableList<EduUserInfo>, roomType: RoomType):
                MutableList<EduUserInfo> {
            val validUserInfoList = mutableListOf<EduUserInfo>()
            synchronized(userInfoList) {
                for (element in onlineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val userInfo1 = EduUserInfoImpl(element.userUuid, element.userName, role,
                            element.muteChat == EduChatState.Allow.value, element.updateTime)
                    if (userInfoList.contains(userInfo1)) {
                        val index = userInfoList.indexOf(userInfo1)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = userInfoList[index]
                        if (compareUserInfoTime(userInfo1, userInfo2) > 0) {
                            /**更新用户的数据为最新数据*/
                            userInfoList[index] = userInfo1
                            validUserInfoList.add(userInfo1)
                        }
                    } else {
                        userInfoList.add(userInfo1)
                        validUserInfoList.add(userInfo1)
                    }
                }
            }
            return validUserInfoList
        }

        fun modifyUserWithUserStateChange(cmdUserStateMsg: CMDUserStateMsg,
                                          eduUserInfos: MutableList<EduUserInfo>, roomType: RoomType)
                : MutableList<EduUserEvent> {
            val userStateChangedList = mutableListOf<EduUserInfo>()
            userStateChangedList.add(Convert.convertUserInfo(cmdUserStateMsg, roomType))
            val validUserEventList = mutableListOf<EduUserEvent>()
            synchronized(eduUserInfos) {
                for (element in userStateChangedList) {
                    if (eduUserInfos.contains(element)) {
                        val index = eduUserInfos.indexOf(element)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = eduUserInfos[index]
                        if (compareUserInfoTime(element, userInfo2) > 0) {
                            /**更新用户的数据为最新数据*/
                            eduUserInfos[index] = element
                            /**构造userEvent并返回*/
                            val operator = getOperator(cmdUserStateMsg.operator, element, roomType)
                            val userEvent = EduUserEvent(element, operator)
                            validUserEventList.add(userEvent)
                        }
                    }
                }
            }
            return validUserEventList
        }


        /**调用此函数之前须确保first和second代表的是同一个流
         *
         * 比较first的数据是否比second的更为接近当前时间(即找出一个最新数据)
         * @return > 0（first > second）
         *         !(> 0) first <= second*/
        private fun compareStreamInfoTime(first: EduStreamInfo, second: EduStreamInfo): Long {
            /**判断更新时间是否为空(为空的有可能是原始数据)*/
            if ((first as EduStreamInfoImpl).updateTime == null) {
                return -1
            }
            if ((second as EduStreamInfoImpl).updateTime == null) {
                return first.updateTime!!
            }
            /**最终判断出最新数据*/
            return first.updateTime!!.minus(second.updateTime!!)
        }

//        /**operator有可能为空(说明用户自己就是操作者)，我们需要把当前用户设置为操作者*/
//        private fun getOperator(operator: EduFromUserRes?, userInfo: EduUserInfo, roomType: RoomType):
//                EduUserInfo {
//            /**operator为空说明操作者是自己*/
//            var operatorUser: EduUserInfo? = null
//            operator?.let {
//                operatorUser = Convert.convertUserInfo(operator, roomType)
//            }
//            if (operatorUser == null) {
//                operatorUser = userInfo
//            }
//            return operatorUser!!
//        }

        fun addStreamWithAction(cmdStreamActionMsg: CMDStreamActionMsg,
                                streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType):
                MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            val streamInfos = mutableListOf<EduStreamInfo>()
            streamInfos.add(Convert.convertStreamInfo(cmdStreamActionMsg, roomType))
            synchronized(streamInfoList) {
                for (element in streamInfos) {
                    if (streamInfoList.contains(element)) {
                        val index = streamInfoList.indexOf(element)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = streamInfoList[index]
                        if (compareStreamInfoTime(element, userInfo2) > 0) {
                            /**更新用户的数据为最新数据*/
                            streamInfoList[index] = element
                            /**构造userEvent并返回*/
                            val operator = getOperator(cmdStreamActionMsg.operator, element.publisher, roomType)
                            val userEvent = EduStreamEvent(element, operator)
                            validStreamList.add(userEvent)
                        }
                    } else {
                        streamInfoList.add(element)
                        /**构造userEvent并返回*/
                        val operator = getOperator(cmdStreamActionMsg.operator, element.publisher, roomType)
                        val userEvent = EduStreamEvent(element, operator)
                        validStreamList.add(userEvent)
                    }
                }
            }
            return validStreamList
        }

        fun modifyStreamWithAction(cmdStreamActionMsg: CMDStreamActionMsg,
                                   streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType):
                MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            val streamInfos = mutableListOf<EduStreamInfo>()
            streamInfos.add(Convert.convertStreamInfo(cmdStreamActionMsg, roomType))
            synchronized(streamInfoList) {
                for (element in streamInfos) {
                    if (streamInfoList.contains(element)) {
                        val index = streamInfoList.indexOf(element)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = streamInfoList[index]
                        if (compareStreamInfoTime(element, userInfo2) > 0) {
                            /**更新用户的数据为最新数据*/
                            streamInfoList[index] = element
                            /**构造userEvent并返回*/
                            val operator = getOperator(cmdStreamActionMsg.operator, element.publisher, roomType)
                            val userEvent = EduStreamEvent(element, operator)
                            validStreamList.add(userEvent)
                        }
                    }
                }
            }
            return validStreamList
        }

        fun removeStreamWithAction(cmdStreamActionMsg: CMDStreamActionMsg,
                                   streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType):
                MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            val streamInfos = mutableListOf<EduStreamInfo>()
            streamInfos.add(Convert.convertStreamInfo(cmdStreamActionMsg, roomType))
            synchronized(streamInfoList) {
                for (element in streamInfos) {
                    if (streamInfoList.contains(element)) {
                        val index = streamInfoList.indexOf(element)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = streamInfoList[index]
                        if (compareStreamInfoTime(element, userInfo2) > 0) {
                            /**更新用户的数据为最新数据*/
                            streamInfoList.removeAt(index)
                            /**构造userEvent并返回*/
                            val operator = getOperator(cmdStreamActionMsg.operator, element.publisher, roomType)
                            val userEvent = EduStreamEvent(element, operator)
                            validStreamList.add(userEvent)
                        }
                    }
                }
            }
            return validStreamList
        }

        fun removeStreamWithUserLeave(removedUserEvents: MutableList<EduUserEvent>,
                                      streamInfoList: MutableList<EduStreamInfo>): MutableList<EduStreamEvent> {
            val removedStreams = mutableListOf<EduStreamEvent>()
            synchronized(streamInfoList) {
                for (element in streamInfoList) {
                    val publisher = element.publisher
                    for (userEvent in removedUserEvents) {
                        if (publisher == userEvent.modifiedUser) {
                            /**移除流*/
                            streamInfoList.remove(element)
                            removedStreams.add(EduStreamEvent(element, userEvent.operatorUser))
                        }
                    }
                }
            }
            return removedStreams
        }




        /**把RTM通知过来的房间信息同步至eduRoom中
         * @return 房间中那种信息发生了改变
         *     RoomStatusEvent.COURSE_STATE : 课堂信息(自定义信息或状态)发生了改变
         *     RoomStatusEvent.STUDENT_STATE : 课堂中关于学生的设置发生了改变
         *     null   没有任何改变发生*/
        fun syncRoomInfoToEduRoom(roomInfoRes: CMDSyncRoomInfoRes, eduRoom: EduRoom): RoomStatusEvent? {
            var event: RoomStatusEvent? = null
            /**roomUuid和roomName是final，也不会被改变，不用同步*/
            if (eduRoom.roomProperties != roomInfoRes.roomProperties) {
                eduRoom.roomProperties = roomInfoRes.roomProperties
                event = RoomStatusEvent.COURSE_STATE
            }
            val roomState = roomInfoRes.roomState
            val courseState = Convert.convertRoomState(roomState?.state!!)
            if (eduRoom.roomStatus.courseState != courseState) {
                eduRoom.roomStatus.courseState = courseState
                event = RoomStatusEvent.COURSE_STATE
            }
            if (eduRoom.roomStatus.startTime != roomState.startTime) {
                eduRoom.roomStatus.startTime = roomState.startTime
                event = RoomStatusEvent.COURSE_STATE
            }
            val isStudentChatAllowed = Convert.extractStudentChatAllowState(roomState,
                    (eduRoom as EduRoomImpl).getCurRoomType())
            if (eduRoom.roomStatus.isStudentChatAllowed != isStudentChatAllowed) {
                eduRoom.roomStatus.isStudentChatAllowed = isStudentChatAllowed
                event = RoomStatusEvent.STUDENT_CHAT
            }
            return event
        }

        /**把RTM通知过来的全量人流信息同步至eduRoom中
         * 第一阶段（根据nextId同步全量数据），如果中间断连，可根据nextId续传
         * 因为第一阶段是全量，所以不用校验updateTime，直接全量add
         * @return 当前处理的最后一条数据*/
        fun syncUserStreamListToEduRoomWithFirst(userStreamRes: CMDSyncUserStreamRes, eduRoom: EduRoom) {
            userStreamRes.list?.let {
                for (element in userStreamRes.list) {
                    val role = Convert.convertUserRole(element.role, (eduRoom as EduRoomImpl).getCurRoomType())
                    val eduUserInfo: EduUserInfoImpl = EduUserInfoImpl(element.userUuid, element.userName, role,
                            element.muteChat == EduChatState.Allow.value, element.updateTime)
                    /**设置可能存在的用户自定义数据*/
                    eduUserInfo.userProperties = element.userProperties
                    (eduRoom as EduRoomImpl).getCurUserList().add(eduUserInfo)
                    element.streams?.let {
                        for (syncStreamRes in element.streams) {
                            val eduStreamInfo: EduStreamInfo = Convert.convertStreamInfo(syncStreamRes, eduUserInfo)
                            (eduRoom as EduRoomImpl).getCurStreamList().add(eduStreamInfo)
                        }
                    }
                }
            }
        }

        /**把RTM通知过来的增量人流信息同步至eduRoom中
         * 第二阶段（根据ts增量），如果中间断连，可根据ts续传
         * 第二阶段是增量数据，所以我们需要校验updateTime
         * @return 第二阶段的增量数据是否同步完成*/
        fun syncUserStreamListToEduRoomWithSecond(userStreamRes: CMDSyncUserStreamRes, eduRoom: EduRoom)
                : Array<MutableList<Any>> {
            val validOnlineUserList = mutableListOf<Any>()
            val validModifiedUserList = mutableListOf<Any>()
            val validOfflineUserList = mutableListOf<Any>()
            val validAddedStreamList = mutableListOf<Any>()
            val validModifiedStreamList = mutableListOf<Any>()
            val validRemovedStreamList = mutableListOf<Any>()

            val eduUserList = (eduRoom as EduRoomImpl).getCurUserList()
            val eduStreamList = (eduRoom as EduRoomImpl).getCurStreamList()
            userStreamRes.list?.let {
            }
            userStreamRes.list?.forEach {
                for ((index, element) in userStreamRes.list.withIndex()) {
                    val role = Convert.convertUserRole(element.role, (eduRoom as EduRoomImpl).getCurRoomType())
                    val eduUserInfo: EduUserInfo = EduUserInfoImpl(element.userUuid, element.userName, role,
                            element.muteChat == EduChatState.Allow.value, element.updateTime)
                    /**设置可能存在的用户自定义数据*/
                    eduUserInfo.userProperties = element.userProperties
                    if (element.state == CMDUserState.Online.value) {
                        if (eduUserList.contains(eduUserInfo)) {
                            /**本地包含此用户，比较数据得更新时间*/
                            if (compareUserInfoTime(eduUserInfo, eduUserList[index]) > 0) {
                                /**本地包含此用户，说明是用户数据更新;*/
                                val pos = eduUserList.indexOf(eduUserInfo)
                                eduUserList[pos] = eduUserInfo
                                validModifiedUserList.add(EduUserEvent(eduUserInfo, null))
                                /**还需判断所属流是否存在于本地*/
                                element.streams?.let {
                                    for ((pos, streamRes) in element.streams.withIndex()) {
                                        val eduStreamInfo: EduStreamInfo = Convert.convertStreamInfo(
                                                streamRes, eduUserInfo)
                                        /**存在则为更新，不存在则为添加*/
                                        if (eduStreamList.contains(eduStreamInfo)) {
                                            eduStreamList[pos] = eduStreamInfo
                                            validModifiedStreamList.add(EduStreamEvent(eduStreamInfo, null))
                                        } else {
                                            eduStreamList.add(eduStreamInfo)
                                            validAddedStreamList.add(EduStreamEvent(eduStreamInfo, null))
                                        }
                                    }
                                }
                            } else {
                                /**用户数据得更新时间比较晚，则不处理用户数据和流数据*/
                            }
                        } else {
                            /**本地不包含此用户，说明是新增的人，那么对应的流也是新增的*/
                            eduUserList.add(eduUserInfo)
                            validOnlineUserList.add(eduUserInfo)
                            element.streams?.let {
                                for (addedStream in element.streams) {
                                    val eduStreamInfo: EduStreamInfo = Convert.convertStreamInfo(
                                            addedStream, eduUserInfo)
                                    eduStreamList.add(eduStreamInfo)
                                    validAddedStreamList.add(EduStreamEvent(eduStreamInfo, null))
                                }
                            }
                        }
                    } else if (element.state == CMDUserState.Offline.value) {
                        /**下线用户不存在与本地缓存中，那么就不是有效数据*/
                        if (eduUserList.contains(eduUserInfo)) {
                            /**判断更新时间,获取最新数据；更新本地缓存数据*/
                            if (compareUserInfoTime(eduUserInfo, eduUserList[index]) > 0) {
                                eduUserList.removeAt(index)
                                validOfflineUserList.add(EduUserEvent(eduUserInfo, null))
                                /**用户所属的流判定为remove;更新本地缓存数据*/
                                element.streams?.let {
                                    for (removedStream in element.streams) {
                                        val eduStreamInfo: EduStreamInfo = Convert.convertStreamInfo(
                                                removedStream, eduUserInfo)
                                        eduStreamList.remove(eduStreamInfo)
                                        validRemovedStreamList.add(EduStreamEvent(eduStreamInfo, null))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return arrayOf(validOnlineUserList, validModifiedUserList, validOfflineUserList,
                    validAddedStreamList, validModifiedStreamList, validRemovedStreamList)
        }
    }
}
