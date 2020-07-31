package io.agora.education.impl.cmd

import io.agora.Convert
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.RoomStatusEvent
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduChatState
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.response.EduFromUserRes
import io.agora.education.impl.room.data.response.EduUserRes
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.data.EduUserInfoImpl

class CMDProcessor {
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
            if (eduRoom.roomInfo.roomProperties != roomInfoRes.roomInfo.roomProperties) {
                eduRoom.roomInfo.roomProperties = roomInfoRes.roomInfo.roomProperties
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

        /**把RTM通知过来的人流信息同步至eduRoom中*/
        fun syncUsrStreamListToEduRoom(useStreamRes: CMDSyncUsrStreamRes, eduRoom: EduRoom) {

        }
    }
}
